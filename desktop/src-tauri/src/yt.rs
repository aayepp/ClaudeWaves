//! YouTube Music metadata through the InnerTube JSON API used by the
//! music.youtube.com web client: home feed, search, artist / album /
//! playlist pages and the "up next" radio queue.
//!
//! Responses are walked generically: every shelf container becomes a
//! `Shelf`, every list or card renderer inside it becomes an `Item`. That keeps
//! working when YouTube reorders page layouts, which it does often.

use std::sync::Mutex;

use serde::Serialize;
use serde_json::{json, Value};

const BASE: &str = "https://music.youtube.com/youtubei/v1";
const CLIENT_VERSION: &str = "1.20250310.01.00";
const USER_AGENT: &str =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

#[derive(Serialize, Clone, Debug, Default)]
#[serde(rename_all = "camelCase")]
pub struct Ref {
    pub name: String,
    pub id: Option<String>,
}

#[derive(Serialize, Clone, Debug, Default)]
#[serde(rename_all = "camelCase")]
pub struct Item {
    /// song | video | album | artist | playlist | podcast | episode
    pub kind: String,
    pub video_id: Option<String>,
    pub browse_id: Option<String>,
    pub playlist_id: Option<String>,
    pub title: String,
    pub subtitle: String,
    pub artists: Vec<Ref>,
    pub album: Option<Ref>,
    pub duration: Option<f64>,
    pub thumbnail: Option<String>,
    pub explicit: bool,
}

#[derive(Serialize, Clone, Debug, Default)]
#[serde(rename_all = "camelCase")]
pub struct Shelf {
    pub title: String,
    /// Browse id of the shelf's "More" link, when it has one.
    pub more: Option<String>,
    pub items: Vec<Item>,
}

#[derive(Serialize, Clone, Debug, Default)]
#[serde(rename_all = "camelCase")]
pub struct Header {
    pub title: String,
    pub subtitle: String,
    pub second_subtitle: String,
    pub description: String,
    pub thumbnail: Option<String>,
    pub artists: Vec<Ref>,
    /// Playlist id to play the whole page (album or playlist), when known.
    pub play_playlist_id: Option<String>,
}

#[derive(Serialize, Clone, Debug, Default)]
#[serde(rename_all = "camelCase")]
pub struct Chip {
    pub title: String,
    pub browse_id: Option<String>,
    pub params: Option<String>,
}

#[derive(Serialize, Clone, Debug, Default)]
#[serde(rename_all = "camelCase")]
pub struct Page {
    pub header: Option<Header>,
    pub shelves: Vec<Shelf>,
    pub chips: Vec<Chip>,
}

pub struct YtClient {
    http: reqwest::Client,
    visitor: Mutex<Option<String>>,
    pub hl: Mutex<String>,
    pub gl: Mutex<String>,
}

impl YtClient {
    pub fn new() -> Self {
        let http = reqwest::Client::builder()
            .user_agent(USER_AGENT)
            .gzip(true)
            .timeout(std::time::Duration::from_secs(20))
            .build()
            .expect("http client");
        Self { http, visitor: Mutex::new(None), hl: Mutex::new("en".into()), gl: Mutex::new("MY".into()) }
    }

    async fn call(&self, endpoint: &str, mut body: Value) -> Result<Value, String> {
        let hl = self.hl.lock().map(|g| g.clone()).unwrap_or_else(|_| "en".into());
        let gl = self.gl.lock().map(|g| g.clone()).unwrap_or_else(|_| "MY".into());
        let visitor = self.visitor.lock().ok().and_then(|g| g.clone());
        let mut client = json!({ "clientName": "WEB_REMIX", "clientVersion": CLIENT_VERSION, "hl": hl, "gl": gl });
        if let Some(v) = &visitor {
            client["visitorData"] = json!(v);
        }
        body["context"] = json!({ "client": client });
        let mut req = self
            .http
            .post(format!("{BASE}/{endpoint}?prettyPrint=false"))
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .json(&body);
        if let Some(v) = &visitor {
            req = req.header("X-Goog-Visitor-Id", v);
        }
        let res = req.send().await.map_err(|e| format!("Network error: {e}"))?;
        let status = res.status();
        let value: Value = res.json().await.map_err(|e| format!("Bad response: {e}"))?;
        if !status.is_success() {
            let msg = value.pointer("/error/message").and_then(Value::as_str).unwrap_or("request failed");
            return Err(format!("YouTube Music: {msg} ({status})"));
        }
        if visitor.is_none() {
            if let Some(v) = value.pointer("/responseContext/visitorData").and_then(Value::as_str) {
                if let Ok(mut g) = self.visitor.lock() {
                    *g = Some(v.to_string());
                }
            }
        }
        Ok(value)
    }

    pub async fn browse(&self, browse_id: &str, params: Option<&str>) -> Result<Page, String> {
        let mut body = json!({ "browseId": browse_id });
        if let Some(p) = params {
            body["params"] = json!(p);
        }
        let v = self.call("browse", body).await?;
        Ok(parse_page(&v))
    }

    /// Home feed. Signed-out home is thin, so explore and charts shelves are
    /// appended to give something to play on first launch.
    pub async fn home(&self, params: Option<&str>) -> Result<Page, String> {
        let mut page = self.browse("FEmusic_home", params).await?;
        if params.is_some() {
            return Ok(page);
        }
        let playable = |p: &Page| p.shelves.iter().map(|s| s.items.len()).sum::<usize>();
        if playable(&page) < 40 {
            for extra in ["FEmusic_explore", "FEmusic_charts"] {
                if let Ok(p) = self.browse(extra, None).await {
                    page.shelves.extend(p.shelves);
                }
            }
        }
        page.shelves.retain(|s| !s.items.is_empty());
        // Music first: podcast shelves go to the end.
        page.shelves.sort_by_key(|s| s.title.to_lowercase().contains("podcast") || s.items.iter().all(|i| i.kind == "podcast"));
        Ok(page)
    }

    pub async fn search(&self, query: &str, filter: Option<&str>) -> Result<Page, String> {
        let mut body = json!({ "query": query });
        if let Some(p) = filter.and_then(filter_params) {
            body["params"] = json!(p);
        }
        let v = self.call("search", body).await?;
        let mut page = parse_page(&v);
        // Search results come as many untitled sections; fold them together.
        let mut merged: Vec<Shelf> = Vec::new();
        for s in page.shelves.drain(..) {
            match merged.last_mut() {
                Some(last) if s.title.is_empty() && last.title.is_empty() => last.items.extend(s.items),
                _ => merged.push(s),
            }
        }
        page.shelves = merged;
        Ok(page)
    }

    pub async fn suggestions(&self, query: &str) -> Result<Vec<String>, String> {
        let v = self.call("music/get_search_suggestions", json!({ "input": query })).await?;
        let mut out = Vec::new();
        walk_find_all(&v, "searchSuggestionRenderer", &mut |r| {
            let t = runs_text(r.get("suggestion"));
            if !t.is_empty() {
                out.push(t);
            }
        });
        Ok(out)
    }

    /// The radio / up-next queue for a song.
    pub async fn next(&self, video_id: &str, playlist_id: Option<&str>) -> Result<Vec<Item>, String> {
        let mut body = json!({ "videoId": video_id, "isAudioOnly": true, "enablePersistentPlaylistPanel": true });
        body["playlistId"] = json!(playlist_id.map(str::to_string).unwrap_or_else(|| format!("RDAMVM{video_id}")));
        let v = self.call("next", body).await?;
        let mut items = Vec::new();
        walk_find_all(&v, "playlistPanelVideoRenderer", &mut |r| {
            if let Some(it) = parse_panel_video(r) {
                items.push(it);
            }
        });
        Ok(items)
    }
}

fn filter_params(filter: &str) -> Option<&'static str> {
    Some(match filter {
        "songs" => "EgWKAQIIAWoMEA4QChADEAQQCRAF",
        "videos" => "EgWKAQIQAWoMEA4QChADEAQQCRAF",
        "albums" => "EgWKAQIYAWoMEA4QChADEAQQCRAF",
        "artists" => "EgWKAQIgAWoMEA4QChADEAQQCRAF",
        "playlists" => "EgeKAQQoAEABagwQDhAKEAMQBBAJEAU%3D",
        _ => return None,
    })
}

// ---------------------------------------------------------------------------
// Generic page walker
// ---------------------------------------------------------------------------

const SHELF_KEYS: &[&str] = &[
    "musicCarouselShelfRenderer",
    "musicShelfRenderer",
    "musicPlaylistShelfRenderer",
    "gridRenderer",
    "itemSectionRenderer",
    "musicCardShelfRenderer",
];

const HEADER_KEYS: &[&str] = &[
    "musicResponsiveHeaderRenderer",
    "musicImmersiveHeaderRenderer",
    "musicVisualHeaderRenderer",
    "musicDetailHeaderRenderer",
    "musicEditablePlaylistDetailHeaderRenderer",
];

pub fn parse_page(v: &Value) -> Page {
    let mut page = Page::default();
    let mut shelves: Vec<Shelf> = vec![Shelf::default()];
    walk(v, &mut shelves, &mut page, 0);
    page.shelves = shelves.into_iter().filter(|s| !s.items.is_empty()).collect();
    page
}

fn walk(v: &Value, shelves: &mut Vec<Shelf>, page: &mut Page, depth: usize) {
    if depth > 64 {
        return;
    }
    match v {
        Value::Object(map) => {
            for (k, child) in map {
                if let Some(item) = match k.as_str() {
                    "musicResponsiveListItemRenderer" => parse_list_item(child),
                    "musicTwoRowItemRenderer" => parse_two_row(child),
                    "playlistPanelVideoRenderer" => parse_panel_video(child),
                    _ => None,
                } {
                    if let Some(s) = shelves.last_mut() {
                        s.items.push(item);
                    }
                    continue;
                }
                if k == "musicCardShelfRenderer" {
                    // The "top result" card: the card itself is an item.
                    shelves.push(Shelf { title: "Top result".into(), ..Default::default() });
                    if let Some(it) = parse_card(child) {
                        shelves.last_mut().unwrap().items.push(it);
                    }
                    walk(child.get("contents").unwrap_or(&Value::Null), shelves, page, depth + 1);
                    shelves.push(Shelf::default());
                    continue;
                }
                if SHELF_KEYS.contains(&k.as_str()) {
                    shelves.push(Shelf { title: shelf_title(child), more: shelf_more(child), items: vec![] });
                    walk(child, shelves, page, depth + 1);
                    shelves.push(Shelf::default());
                    continue;
                }
                if page.header.is_none() && HEADER_KEYS.contains(&k.as_str()) {
                    page.header = Some(parse_header(child));
                    continue;
                }
                if k == "chipCloudChipRenderer" {
                    let title = runs_text(child.get("text"));
                    let ep = child.get("navigationEndpoint");
                    page.chips.push(Chip {
                        title,
                        browse_id: ep.and_then(|e| e.pointer("/browseEndpoint/browseId")).and_then(Value::as_str).map(String::from),
                        params: ep.and_then(|e| e.pointer("/browseEndpoint/params")).and_then(Value::as_str).map(String::from),
                    });
                    continue;
                }
                // Menus, overlays and tracking blobs never hold content items.
                if matches!(k.as_str(), "menu" | "trackingParams" | "overlay" | "thumbnailOverlay" | "frameworkUpdates") {
                    continue;
                }
                walk(child, shelves, page, depth + 1);
            }
        }
        Value::Array(arr) => {
            for child in arr {
                walk(child, shelves, page, depth + 1);
            }
        }
        _ => {}
    }
}

fn walk_find_all(v: &Value, key: &str, f: &mut dyn FnMut(&Value)) {
    match v {
        Value::Object(map) => {
            for (k, child) in map {
                if k == key {
                    f(child);
                } else {
                    walk_find_all(child, key, f);
                }
            }
        }
        Value::Array(arr) => arr.iter().for_each(|c| walk_find_all(c, key, f)),
        _ => {}
    }
}

fn shelf_title(r: &Value) -> String {
    for p in [
        "/header/musicCarouselShelfBasicHeaderRenderer/title",
        "/header/musicShelfBasicHeaderRenderer/title",
        "/header/gridHeaderRenderer/title",
        "/title",
    ] {
        let t = runs_text(r.pointer(p));
        if !t.is_empty() {
            return t;
        }
    }
    String::new()
}

fn shelf_more(r: &Value) -> Option<String> {
    for p in [
        "/header/musicCarouselShelfBasicHeaderRenderer/moreContentButton/buttonRenderer/navigationEndpoint/browseEndpoint/browseId",
        "/header/musicCarouselShelfBasicHeaderRenderer/title/runs/0/navigationEndpoint/browseEndpoint/browseId",
        "/bottomEndpoint/browseEndpoint/browseId",
        "/title/runs/0/navigationEndpoint/browseEndpoint/browseId",
    ] {
        if let Some(s) = r.pointer(p).and_then(Value::as_str) {
            return Some(s.to_string());
        }
    }
    None
}

// ---------------------------------------------------------------------------
// Item parsers
// ---------------------------------------------------------------------------

fn runs(v: Option<&Value>) -> Vec<&Value> {
    v.and_then(|t| t.get("runs")).and_then(Value::as_array).map(|a| a.iter().collect()).unwrap_or_default()
}

pub fn runs_text(v: Option<&Value>) -> String {
    let rs = runs(v);
    if rs.is_empty() {
        return v.and_then(|t| t.get("simpleText")).and_then(Value::as_str).unwrap_or("").to_string();
    }
    rs.iter().filter_map(|r| r.get("text").and_then(Value::as_str)).collect()
}

fn best_thumb(v: Option<&Value>) -> Option<String> {
    let v = v?;
    let list = [
        "/musicThumbnailRenderer/thumbnail/thumbnails",
        "/croppedSquareThumbnailRenderer/thumbnail/thumbnails",
        "/thumbnails",
        "/thumbnail/thumbnails",
    ]
    .iter()
    .find_map(|p| v.pointer(p).and_then(Value::as_array))?;
    let url = list.last()?.get("url")?.as_str()?;
    Some(upscale(url))
}

/// Google image URLs carry their size in the URL; ask for a bigger one.
fn upscale(url: &str) -> String {
    if url.contains("googleusercontent.com") || url.contains("ggpht.com") {
        if let Some(i) = url.rfind('=') {
            return format!("{}=w544-h544-l90-rj", &url[..i]);
        }
    }
    url.to_string()
}

pub fn parse_duration(s: &str) -> Option<f64> {
    let parts: Vec<&str> = s.trim().split(':').collect();
    if parts.len() < 2 || parts.len() > 3 || !parts.iter().all(|p| !p.is_empty() && p.chars().all(|c| c.is_ascii_digit())) {
        return None;
    }
    Some(parts.iter().fold(0.0, |acc, p| acc * 60.0 + p.parse::<f64>().unwrap_or(0.0)))
}

fn page_type(ep: Option<&Value>) -> Option<&str> {
    ep?.pointer("/browseEndpoint/browseEndpointContextSupportedConfigs/browseEndpointContextMusicConfig/pageType")?.as_str()
}

fn kind_from_page_type(pt: &str) -> Option<&'static str> {
    Some(match pt {
        "MUSIC_PAGE_TYPE_ARTIST" | "MUSIC_PAGE_TYPE_USER_CHANNEL" | "MUSIC_PAGE_TYPE_LIBRARY_ARTIST" => "artist",
        "MUSIC_PAGE_TYPE_ALBUM" | "MUSIC_PAGE_TYPE_AUDIOBOOK" => "album",
        "MUSIC_PAGE_TYPE_PLAYLIST" => "playlist",
        "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE" => "podcast",
        "MUSIC_PAGE_TYPE_NON_MUSIC_AUDIO_TRACK_PAGE" => "episode",
        _ => return None,
    })
}

fn kind_from_video_type(vt: Option<&str>) -> &'static str {
    match vt {
        Some("MUSIC_VIDEO_TYPE_ATV") => "song",
        Some("MUSIC_VIDEO_TYPE_PODCAST_EPISODE") => "episode",
        Some(_) => "video",
        None => "song",
    }
}

fn watch_of(v: &Value) -> Option<&Value> {
    for p in [
        "/overlay/musicItemThumbnailOverlayRenderer/content/musicPlayButtonRenderer/playNavigationEndpoint/watchEndpoint",
        "/thumbnailOverlay/musicItemThumbnailOverlayRenderer/content/musicPlayButtonRenderer/playNavigationEndpoint/watchEndpoint",
        "/navigationEndpoint/watchEndpoint",
        "/flexColumns/0/musicResponsiveListItemFlexColumnRenderer/text/runs/0/navigationEndpoint/watchEndpoint",
        "/title/runs/0/navigationEndpoint/watchEndpoint",
    ] {
        if let Some(w) = v.pointer(p) {
            return Some(w);
        }
    }
    None
}

fn watch_playlist_of(v: &Value) -> Option<String> {
    for p in [
        "/overlay/musicItemThumbnailOverlayRenderer/content/musicPlayButtonRenderer/playNavigationEndpoint/watchPlaylistEndpoint/playlistId",
        "/thumbnailOverlay/musicItemThumbnailOverlayRenderer/content/musicPlayButtonRenderer/playNavigationEndpoint/watchPlaylistEndpoint/playlistId",
        "/overlay/musicItemThumbnailOverlayRenderer/content/musicPlayButtonRenderer/playNavigationEndpoint/watchEndpoint/playlistId",
    ] {
        if let Some(s) = v.pointer(p).and_then(Value::as_str) {
            return Some(s.to_string());
        }
    }
    None
}

/// Pulls artists, album and duration out of subtitle runs and decides the
/// subtitle text without the leading type label ("Song • ").
fn read_subtitle(rs: &[&Value], item: &mut Item) {
    let mut parts: Vec<String> = Vec::new();
    for r in rs {
        let text = r.get("text").and_then(Value::as_str).unwrap_or("");
        let ep = r.get("navigationEndpoint");
        match page_type(ep).and_then(kind_from_page_type) {
            Some("artist") => item.artists.push(Ref {
                name: text.to_string(),
                id: ep.and_then(|e| e.pointer("/browseEndpoint/browseId")).and_then(Value::as_str).map(String::from),
            }),
            Some("album") => {
                item.album = Some(Ref {
                    name: text.to_string(),
                    id: ep.and_then(|e| e.pointer("/browseEndpoint/browseId")).and_then(Value::as_str).map(String::from),
                })
            }
            _ => {}
        }
        if item.duration.is_none() {
            item.duration = parse_duration(text);
        }
        parts.push(text.to_string());
    }
    let joined: String = parts.concat();
    let segs: Vec<&str> = joined.split(" • ").map(str::trim).filter(|s| !s.is_empty()).collect();
    let label = segs.first().copied().unwrap_or("");
    let labelled = match label {
        "Song" => Some("song"),
        "Video" => Some("video"),
        "Album" | "EP" | "Single" => Some("album"),
        "Artist" => Some("artist"),
        "Playlist" => Some("playlist"),
        "Episode" => Some("episode"),
        "Podcast" => Some("podcast"),
        _ => None,
    };
    if let Some(k) = labelled {
        if item.kind.is_empty() {
            item.kind = k.to_string();
        }
    }
    let keep: Vec<&str> = segs
        .into_iter()
        .enumerate()
        .filter(|(i, s)| !(*i == 0 && labelled.is_some() && !matches!(*s, "EP" | "Single")) && parse_duration(s).is_none())
        .map(|(_, s)| s)
        .collect();
    item.subtitle = keep.join(" • ");
    if item.artists.is_empty() {
        // Plain-text artist (no link): take the first remaining segment.
        if let Some(first) = keep.first() {
            if !first.contains(" views") && !first.contains(" plays") && !first.chars().all(|c| c.is_ascii_digit()) {
                item.artists.push(Ref { name: first.to_string(), id: None });
            }
        }
    }
}

fn parse_list_item(r: &Value) -> Option<Item> {
    let cols = r.get("flexColumns")?.as_array()?;
    let col = |i: usize| cols.get(i).and_then(|c| c.pointer("/musicResponsiveListItemFlexColumnRenderer/text"));
    let title = runs_text(col(0));
    if title.is_empty() {
        return None;
    }
    let mut item = Item { title, thumbnail: best_thumb(r.get("thumbnail")), ..Default::default() };

    let watch = watch_of(r);
    item.video_id = r
        .pointer("/playlistItemData/videoId")
        .and_then(Value::as_str)
        .map(String::from)
        .or_else(|| watch.and_then(|w| w.get("videoId")).and_then(Value::as_str).map(String::from));
    let nav = r.get("navigationEndpoint");
    if let Some(bid) = nav.and_then(|n| n.pointer("/browseEndpoint/browseId")).and_then(Value::as_str) {
        item.browse_id = Some(bid.to_string());
        if let Some(k) = page_type(nav).and_then(kind_from_page_type) {
            item.kind = k.to_string();
        }
    }
    item.playlist_id = watch_playlist_of(r);

    let mut sub: Vec<&Value> = Vec::new();
    for i in 1..cols.len() {
        let rs = runs(col(i));
        if !rs.is_empty() && !sub.is_empty() {
            sub.push(&Value::Null);
        }
        sub.extend(rs);
    }
    // Rebuild with explicit separators between columns.
    let sep = json!({ "text": " • " });
    let sub: Vec<&Value> = sub.into_iter().map(|v| if v.is_null() { &sep } else { v }).collect();
    read_subtitle(&sub, &mut item);

    if item.duration.is_none() {
        let fixed = runs_text(r.pointer("/fixedColumns/0/musicResponsiveListItemFixedColumnRenderer/text"));
        item.duration = parse_duration(&fixed);
    }
    if item.kind.is_empty() {
        let vt = watch
            .and_then(|w| w.pointer("/watchEndpointMusicSupportedConfigs/watchEndpointMusicConfig/musicVideoType"))
            .and_then(Value::as_str);
        item.kind = if item.video_id.is_some() { kind_from_video_type(vt) } else { "playlist" }.to_string();
    }
    item.explicit = r.to_string().contains("MUSIC_EXPLICIT_BADGE");
    Some(item)
}

fn parse_two_row(r: &Value) -> Option<Item> {
    let title = runs_text(r.get("title"));
    if title.is_empty() {
        return None;
    }
    let mut item = Item { title, thumbnail: best_thumb(r.get("thumbnailRenderer")), ..Default::default() };
    let nav = r.get("navigationEndpoint");
    if let Some(bid) = nav.and_then(|n| n.pointer("/browseEndpoint/browseId")).and_then(Value::as_str) {
        item.browse_id = Some(bid.to_string());
        if let Some(k) = page_type(nav).and_then(kind_from_page_type) {
            item.kind = k.to_string();
        }
    }
    if let Some(w) = nav.and_then(|n| n.get("watchEndpoint")) {
        item.video_id = w.get("videoId").and_then(Value::as_str).map(String::from);
        let vt = w.pointer("/watchEndpointMusicSupportedConfigs/watchEndpointMusicConfig/musicVideoType").and_then(Value::as_str);
        item.kind = kind_from_video_type(vt).to_string();
    }
    item.playlist_id = watch_playlist_of(r);
    read_subtitle(&runs(r.get("subtitle")), &mut item);
    if item.kind.is_empty() {
        item.kind = "playlist".into();
    }
    item.explicit = r.get("subtitleBadges").map(|b| b.to_string().contains("EXPLICIT")).unwrap_or(false);
    Some(item)
}

fn parse_panel_video(r: &Value) -> Option<Item> {
    let video_id = r.get("videoId").and_then(Value::as_str).map(String::from).or_else(|| {
        r.pointer("/navigationEndpoint/watchEndpoint/videoId").and_then(Value::as_str).map(String::from)
    })?;
    let mut item = Item {
        title: runs_text(r.get("title")),
        video_id: Some(video_id),
        thumbnail: best_thumb(r.get("thumbnail")),
        duration: parse_duration(&runs_text(r.get("lengthText"))),
        ..Default::default()
    };
    let vt = r
        .pointer("/navigationEndpoint/watchEndpoint/watchEndpointMusicSupportedConfigs/watchEndpointMusicConfig/musicVideoType")
        .and_then(Value::as_str);
    item.kind = kind_from_video_type(vt).to_string();
    read_subtitle(&runs(r.get("longBylineText")), &mut item);
    // Byline is "Artist • 584M views • 2.7M likes" for videos; keep the artist only.
    item.subtitle = item.artists.iter().map(|a| a.name.as_str()).collect::<Vec<_>>().join(", ");
    Some(item)
}

fn parse_card(r: &Value) -> Option<Item> {
    let title = runs_text(r.get("title"));
    if title.is_empty() {
        return None;
    }
    let mut item = Item { title, thumbnail: best_thumb(r.get("thumbnail")), ..Default::default() };
    let nav = r.pointer("/title/runs/0/navigationEndpoint");
    if let Some(bid) = nav.and_then(|n| n.pointer("/browseEndpoint/browseId")).and_then(Value::as_str) {
        item.browse_id = Some(bid.to_string());
        item.kind = page_type(nav).and_then(kind_from_page_type).unwrap_or("artist").to_string();
    }
    if let Some(w) = nav.and_then(|n| n.get("watchEndpoint")) {
        item.video_id = w.get("videoId").and_then(Value::as_str).map(String::from);
        let vt = w.pointer("/watchEndpointMusicSupportedConfigs/watchEndpointMusicConfig/musicVideoType").and_then(Value::as_str);
        item.kind = kind_from_video_type(vt).to_string();
    }
    read_subtitle(&runs(r.get("subtitle")), &mut item);
    if item.kind.is_empty() {
        item.kind = "artist".into();
    }
    Some(item)
}

fn parse_header(r: &Value) -> Header {
    let mut h = Header {
        title: runs_text(r.get("title")),
        subtitle: runs_text(r.get("subtitle")),
        second_subtitle: runs_text(r.get("secondSubtitle")),
        thumbnail: best_thumb(r.get("thumbnail")).or_else(|| best_thumb(r.get("foregroundThumbnail"))),
        ..Default::default()
    };
    let desc = runs_text(r.pointer("/description/musicDescriptionShelfRenderer/description"));
    h.description = if desc.is_empty() { runs_text(r.get("description")) } else { desc };
    for rr in runs(r.get("straplineTextOne")).into_iter().chain(runs(r.get("subtitle"))) {
        let ep = rr.get("navigationEndpoint");
        if page_type(ep).and_then(kind_from_page_type) == Some("artist") {
            h.artists.push(Ref {
                name: rr.get("text").and_then(Value::as_str).unwrap_or("").to_string(),
                id: ep.and_then(|e| e.pointer("/browseEndpoint/browseId")).and_then(Value::as_str).map(String::from),
            });
        }
    }
    if h.artists.is_empty() {
        let s = runs_text(r.get("straplineTextOne"));
        if !s.is_empty() {
            h.artists.push(Ref { name: s, id: None });
        }
    }
    let mut pid = None;
    walk_find_all(r, "watchPlaylistEndpoint", &mut |w| {
        if pid.is_none() {
            pid = w.get("playlistId").and_then(Value::as_str).map(String::from);
        }
    });
    if pid.is_none() {
        walk_find_all(r, "watchEndpoint", &mut |w| {
            if pid.is_none() {
                pid = w.get("playlistId").and_then(Value::as_str).map(String::from);
            }
        });
    }
    h.play_playlist_id = pid;
    h
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Live calls against music.youtube.com: `cargo test -- --ignored live`.
    #[tokio::test]
    #[ignore]
    async fn live_pages_parse() {
        let yt = YtClient::new();
        let home = yt.home(None).await.unwrap();
        println!("home shelves: {:?}", home.shelves.iter().map(|s| (s.title.clone(), s.items.len())).collect::<Vec<_>>());
        assert!(home.shelves.len() >= 2);
        let search = yt.search("coldplay", None).await.unwrap();
        for s in &search.shelves {
            for it in s.items.iter().take(3) {
                println!("search [{}] {} | {} | {:?} {:?} {:?}", it.kind, it.title, it.subtitle, it.video_id, it.browse_id, it.duration);
            }
        }
        let artist_id = search.shelves.iter().flat_map(|s| &s.items).find(|i| i.kind == "artist").and_then(|i| i.browse_id.clone()).unwrap();
        let artist = yt.browse(&artist_id, None).await.unwrap();
        println!("artist header: {:?}", artist.header.as_ref().map(|h| (&h.title, &h.subtitle, h.thumbnail.is_some(), &h.play_playlist_id)));
        println!("artist shelves: {:?}", artist.shelves.iter().map(|s| (s.title.clone(), s.items.len(), s.more.clone())).collect::<Vec<_>>());
        assert!(artist.header.is_some());
        let pl = search.shelves.iter().flat_map(|s| &s.items).find(|i| i.kind == "playlist").and_then(|i| i.browse_id.clone()).unwrap();
        let playlist = yt.browse(&pl, None).await.unwrap();
        println!("playlist header: {:?}", playlist.header.as_ref().map(|h| (&h.title, &h.subtitle, &h.second_subtitle, &h.play_playlist_id)));
        let tracks: Vec<_> = playlist.shelves.iter().flat_map(|s| &s.items).collect();
        println!("playlist tracks: {} first: {:?}", tracks.len(), tracks.first().map(|t| (&t.title, &t.subtitle, t.duration, &t.video_id)));
        assert!(!tracks.is_empty());
        let vid = tracks[0].video_id.clone().unwrap();
        let next = yt.next(&vid, None).await.unwrap();
        println!("radio: {} first: {:?}", next.len(), next.get(1).map(|t| (&t.title, &t.subtitle, t.duration)));
        assert!(next.len() > 5);
        let sug = yt.suggestions("coldp").await.unwrap();
        println!("suggestions: {:?}", sug);
    }

    #[test]
    fn durations() {
        assert_eq!(parse_duration("4:16"), Some(256.0));
        assert_eq!(parse_duration("1:02:03"), Some(3723.0));
        assert_eq!(parse_duration("584M views"), None);
        assert_eq!(parse_duration("Song"), None);
    }

    #[test]
    fn upscales_google_images() {
        assert_eq!(
            upscale("https://lh3.googleusercontent.com/abc=w120-h120-l90-rj"),
            "https://lh3.googleusercontent.com/abc=w544-h544-l90-rj"
        );
        assert_eq!(upscale("https://i.ytimg.com/vi/x/hq.jpg"), "https://i.ytimg.com/vi/x/hq.jpg");
    }

    #[test]
    fn parses_list_item_song() {
        let v = json!({
            "musicResponsiveListItemRenderer": {
                "flexColumns": [
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Yellow"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                        {"text": "Song"}, {"text": " • "},
                        {"text": "Coldplay", "navigationEndpoint": {"browseEndpoint": {"browseId": "UC1",
                            "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ARTIST"}}}}},
                        {"text": " • "},
                        {"text": "Parachutes", "navigationEndpoint": {"browseEndpoint": {"browseId": "MPRE1",
                            "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ALBUM"}}}}},
                        {"text": " • "}, {"text": "4:29"}
                    ]}}}
                ],
                "playlistItemData": {"videoId": "yKNxeF4KMsY"},
                "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://lh3.googleusercontent.com/x=w60-h60"}]}}}
            }
        });
        let page = parse_page(&json!({"contents": {"musicShelfRenderer": {"title": {"runs": [{"text": "Songs"}]}, "contents": [v]}}}));
        assert_eq!(page.shelves.len(), 1);
        assert_eq!(page.shelves[0].title, "Songs");
        let it = &page.shelves[0].items[0];
        assert_eq!(it.kind, "song");
        assert_eq!(it.video_id.as_deref(), Some("yKNxeF4KMsY"));
        assert_eq!(it.artists[0].name, "Coldplay");
        assert_eq!(it.album.as_ref().unwrap().id.as_deref(), Some("MPRE1"));
        assert_eq!(it.duration, Some(269.0));
        assert_eq!(it.subtitle, "Coldplay • Parachutes");
        assert!(it.thumbnail.as_deref().unwrap().ends_with("=w544-h544-l90-rj"));
    }
}
