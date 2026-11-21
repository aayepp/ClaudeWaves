//! Lyrics from LRCLIB (lrclib.net), synced in LRC format when available.

use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone, Default)]
#[serde(rename_all = "camelCase")]
pub struct Lyrics {
    pub synced: Option<String>,
    pub plain: Option<String>,
    pub instrumental: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct LrcLib {
    synced_lyrics: Option<String>,
    plain_lyrics: Option<String>,
    instrumental: Option<bool>,
    duration: Option<f64>,
}

impl From<LrcLib> for Lyrics {
    fn from(l: LrcLib) -> Self {
        Lyrics {
            synced: l.synced_lyrics.filter(|s| !s.trim().is_empty()),
            plain: l.plain_lyrics.filter(|s| !s.trim().is_empty()),
            instrumental: l.instrumental.unwrap_or(false),
        }
    }
}

/// Drops "(Official Video)", "[Lyrics]", "- Remastered 2011" style suffixes
/// that YouTube titles carry and LRCLIB entries do not.
pub fn clean_title(title: &str) -> String {
    let mut t = title.to_string();
    for (open, close) in [('(', ')'), ('[', ']')] {
        while let (Some(a), Some(b)) = (t.find(open), t.rfind(close)) {
            if a >= b {
                break;
            }
            let inner = t[a + 1..b].to_lowercase();
            let noise = ["official", "video", "audio", "lyric", "visualizer", "remaster", "hd", "4k", "mv", "live"];
            if noise.iter().any(|w| inner.contains(w)) {
                t.replace_range(a..=b, "");
            } else {
                break;
            }
        }
    }
    if let Some(i) = t.to_lowercase().find(" - remaster") {
        t.truncate(i);
    }
    t.trim().to_string()
}

pub async fn fetch(title: &str, artist: &str, album: &str, duration: f64) -> Result<Lyrics, String> {
    let client = reqwest::Client::builder()
        .user_agent("ClaudeWaves Desktop/0.2")
        .timeout(std::time::Duration::from_secs(12))
        .build()
        .map_err(|e| e.to_string())?;
    let title = clean_title(title);
    let artist = artist.split(&[',', '&'][..]).next().unwrap_or(artist).trim().to_string();

    // Exact match first (title, artist, album, duration within 2 s).
    let mut q: Vec<(&str, String)> = vec![("track_name", title.clone()), ("artist_name", artist.clone())];
    if !album.is_empty() {
        q.push(("album_name", album.to_string()));
    }
    if duration > 0.0 {
        q.push(("duration", format!("{}", duration.round() as i64)));
    }
    if let Ok(res) = client.get("https://lrclib.net/api/get").query(&q).send().await {
        if res.status().is_success() {
            if let Ok(l) = res.json::<LrcLib>().await {
                let l: Lyrics = l.into();
                if l.synced.is_some() || l.plain.is_some() || l.instrumental {
                    return Ok(l);
                }
            }
        }
    }

    // Then search, preferring synced results close to the track length.
    let res = client
        .get("https://lrclib.net/api/search")
        .query(&[("track_name", title.as_str()), ("artist_name", artist.as_str())])
        .send()
        .await
        .map_err(|e| format!("Lyrics unavailable: {e}"))?;
    let mut list: Vec<LrcLib> = res.json().await.map_err(|e| format!("Lyrics unavailable: {e}"))?;
    list.sort_by(|a, b| {
        let score = |l: &LrcLib| {
            let synced = if l.synced_lyrics.is_some() { 0.0 } else { 1000.0 };
            let dist = match (l.duration, duration > 0.0) {
                (Some(d), true) => (d - duration).abs(),
                _ => 30.0,
            };
            synced + dist
        };
        score(a).partial_cmp(&score(b)).unwrap_or(std::cmp::Ordering::Equal)
    });
    Ok(list.into_iter().next().map(Lyrics::from).unwrap_or_default())
}

#[cfg(test)]
mod tests {
    use super::clean_title;

    #[test]
    fn cleans_youtube_titles() {
        assert_eq!(clean_title("Clocks (Official Video)"), "Clocks");
        assert_eq!(clean_title("Yellow [Official Lyric Video]"), "Yellow");
        assert_eq!(clean_title("Fix You - Remastered 2011"), "Fix You");
        assert_eq!(clean_title("Song (feat. Someone)"), "Song (feat. Someone)");
    }
}
