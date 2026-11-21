// YouTube Music screens: home feed, search, and the artist / album /
// playlist pages, all built from the generic shelves the backend returns.

import { useEffect, useMemo, useState } from 'react';
import { GlassButton, GlassMenu, GlassSegmented } from '@zakisheriff/liquid-glass';
import * as I from './icons';
import { api, fmt, isPlayable, ytTrack, type YtItem, type YtPage, type YtShelf } from './api';
import { placeholderArt } from './data';
import type { Player } from './player';

export type Route =
  | { name: 'home' }
  | { name: 'search'; q: string }
  | { name: 'browse'; id: string; params?: string | null }
  | { name: 'songs' }
  | { name: 'albums' }
  | { name: 'album'; key: string }
  | { name: 'folders' };

export type Go = (r: Route) => void;

const ACCENT = '#7C4DFF';

function usePage(load: () => Promise<YtPage>, deps: unknown[]) {
  const [page, setPage] = useState<YtPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let alive = true;
    setPage(null);
    setError(null);
    load()
      .then((p) => alive && setPage(p))
      .catch((e) => alive && setError(String(e)));
    return () => {
      alive = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, attempt]);
  return { page, error, retry: () => setAttempt((a) => a + 1) };
}

function Thumb({ src, round, wide, size }: { src: string | null; round?: boolean; wide?: boolean; size?: number }) {
  return (
    <span
      className={`yt-thumb${round ? ' round' : ''}${wide ? ' wide' : ''}`}
      style={{
        backgroundImage: src ? `url("${src}")` : placeholderArt("yt"),
        width: size,
        height: size,
      }}
    />
  );
}

function Loading() {
  return (
    <div className="loading" role="status">
      <I.SpinnerIcon />
      <span>Loading…</span>
    </div>
  );
}

function Failed({ error, retry }: { error: string; retry: () => void }) {
  return (
    <div className="failed" role="alert">
      <I.AlertIcon />
      <span>{error}</span>
      <GlassButton size="sm" icon={<I.RefreshIcon />} onClick={retry}>Try again</GlassButton>
    </div>
  );
}

function itemMenu(item: YtItem, player: Player, go: Go) {
  const t = ytTrack(item);
  const entries = [
    { label: 'Play next', icon: <I.QueueNextIcon />, onSelect: () => player.playNext([t]) },
    { label: 'Add to queue', icon: <I.QueueIcon />, onSelect: () => player.addToQueue([t]) },
    { label: 'Start radio', icon: <I.RadioIcon />, onSelect: () => player.playRadio(t) },
  ];
  const artist = item.artists.find((a) => a.id);
  if (artist?.id) entries.push({ label: 'Go to artist', icon: <I.ArtistIcon />, onSelect: () => go({ name: 'browse', id: artist.id! }) });
  if (item.album?.id) entries.push({ label: 'Go to album', icon: <I.AlbumIcon />, onSelect: () => go({ name: 'browse', id: item.album!.id! }) });
  return entries;
}

/** One song row: used in shelves, search results and track lists. */
export function SongRow({
  item,
  n,
  onPlay,
  player,
  go,
  fallbackThumb,
}: {
  item: YtItem;
  n?: number;
  onPlay: () => void;
  player: Player;
  go: Go;
  fallbackThumb?: string | null;
}) {
  const active = player.current?.videoId === item.videoId;
  return (
    <div className={`track yt-row${active ? ' is-playing' : ''}`}>
      <button className="yt-row-main" onClick={onPlay} aria-current={active ? 'true' : undefined}>
        {n !== undefined && <span className="track-n">{n}</span>}
        <Thumb src={item.thumbnail ?? fallbackThumb ?? null} size={44} wide={false} />
        <span className="row-text">
          <span className="row-title">
            {item.title}
            {item.explicit && <span className="explicit" aria-label="Explicit">E</span>}
          </span>
          <span className="row-sub">{item.subtitle || item.artists.map((a) => a.name).join(', ')}</span>
        </span>
        {item.duration ? <span className="row-sub track-dur">{fmt(item.duration)}</span> : null}
      </button>
      <GlassMenu aria-label={`More for ${item.title}`} size="sm" icon={<I.MoreIcon />} align="end" items={itemMenu(item, player, go)} />
    </div>
  );
}

function Card({ item, onOpen }: { item: YtItem; onOpen: () => void }) {
  const round = item.kind === 'artist';
  const wide = item.kind === 'video' || item.kind === 'episode';
  return (
    <button className={`yt-card${wide ? ' wide' : ''}`} onClick={onOpen}>
      <Thumb src={item.thumbnail} round={round} wide={wide} />
      <span className={`row-title${round ? ' center' : ''}`}>{item.title}</span>
      <span className={`row-sub${round ? ' center' : ''}`}>{item.subtitle}</span>
    </button>
  );
}

/** Decides how to show a shelf: song lists as rows, everything else as cards. */
export function ShelfView({ shelf, player, go }: { shelf: YtShelf; player: Player; go: Go }) {
  const songs = shelf.items.filter(isPlayable);
  const songShelf = songs.length >= shelf.items.length * 0.7 && shelf.items.every((i) => i.kind !== 'video');
  const open = (item: YtItem, i: number) => {
    if (isPlayable(item)) {
      if (songShelf) player.playList(songs.map((s) => ytTrack(s)), songs.indexOf(item));
      else player.playRadio(ytTrack(item));
    } else if (item.browseId) go({ name: 'browse', id: item.browseId });
    void i;
  };
  return (
    <section className="shelf">
      {shelf.title && (
        <div className="section-row">
          <h2>{shelf.title}</h2>
          {shelf.more && (
            <GlassButton variant="clear" size="sm" onClick={() => go({ name: 'browse', id: shelf.more! })}>
              More
            </GlassButton>
          )}
        </div>
      )}
      {songShelf ? (
        <div className="song-grid">
          {shelf.items.slice(0, 16).map((item, i) => (
            <SongRow key={`${item.videoId}-${i}`} item={item} player={player} go={go} onPlay={() => open(item, i)} />
          ))}
        </div>
      ) : (
        <div className="carousel" role="list">
          {shelf.items.map((item, i) => (
            <Card key={`${item.browseId ?? item.videoId}-${i}`} item={item} onOpen={() => open(item, i)} />
          ))}
        </div>
      )}
    </section>
  );
}

function greeting() {
  const h = new Date().getHours();
  return h < 5 ? 'Good night' : h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
}

export function YtHome({ player, go }: { player: Player; go: Go }) {
  const [mood, setMood] = useState<string | null>(null);
  const { page, error, retry } = usePage(() => api.ytHome(mood), [mood]);
  const [chips, setChips] = useState<YtPage['chips']>([]);
  useEffect(() => {
    if (page?.chips.length && !mood) setChips(page.chips);
  }, [page, mood]);

  return (
    <>
      <div className="content-head">
        <div>
          <div className="date">{new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })}</div>
          <h1>{greeting()}</h1>
        </div>
      </div>
      {chips.length > 0 && (
        <div className="chips" role="toolbar" aria-label="Moods">
          <button className={`chip-btn${mood === null ? ' is-on' : ''}`} onClick={() => setMood(null)}>For you</button>
          {chips.map((c) => (
            <button key={c.title} className={`chip-btn${mood === c.params ? ' is-on' : ''}`} onClick={() => setMood(c.params)}>
              {c.title}
            </button>
          ))}
        </div>
      )}
      {error ? <Failed error={error} retry={retry} /> : !page ? <Loading /> : page.shelves.map((s, i) => <ShelfView key={`${s.title}-${i}`} shelf={s} player={player} go={go} />)}
    </>
  );
}

const FILTERS: Array<[string | null, string]> = [
  [null, 'All'],
  ['songs', 'Songs'],
  ['videos', 'Videos'],
  ['albums', 'Albums'],
  ['artists', 'Artists'],
  ['playlists', 'Playlists'],
];

export function YtSearch({ q, player, go }: { q: string; player: Player; go: Go }) {
  const [filter, setFilter] = useState(0);
  const { page, error, retry } = usePage(() => api.ytSearch(q, FILTERS[filter][0]), [q, filter]);
  return (
    <>
      <div className="content-head">
        <div>
          <div className="date">Search</div>
          <h1>“{q}”</h1>
        </div>
        <GlassSegmented aria-label="Filter" options={FILTERS.map((f) => f[1])} value={filter} onChange={setFilter} />
      </div>
      {error ? (
        <Failed error={error} retry={retry} />
      ) : !page ? (
        <Loading />
      ) : page.shelves.length === 0 ? (
        <p className="row-sub">No results. Try other words or another filter.</p>
      ) : (
        page.shelves.map((s, i) =>
          s.title === 'Top result' ? (
            <TopResult key={i} item={s.items[0]} player={player} go={go} />
          ) : (
            <ResultsList key={`${s.title}-${i}`} shelf={s} player={player} go={go} />
          ),
        )
      )}
    </>
  );
}

function TopResult({ item, player, go }: { item: YtItem; player: Player; go: Go }) {
  if (!item) return null;
  const open = () => (isPlayable(item) ? player.playRadio(ytTrack(item)) : item.browseId && go({ name: 'browse', id: item.browseId }));
  return (
    <section className="shelf">
      <h2>Top result</h2>
      <button className="top-result" onClick={open}>
        <Thumb src={item.thumbnail} round={item.kind === 'artist'} size={96} />
        <span className="row-text">
          <span className="top-title">{item.title}</span>
          <span className="row-sub">{[item.kind[0].toUpperCase() + item.kind.slice(1), item.subtitle].filter(Boolean).join(' • ')}</span>
        </span>
      </button>
    </section>
  );
}

/** Search results: rows for everything, since mixed result types read best as a list. */
function ResultsList({ shelf, player, go }: { shelf: YtShelf; player: Player; go: Go }) {
  const songs = shelf.items.filter(isPlayable);
  return (
    <section className="shelf">
      {shelf.title && <h2>{shelf.title}</h2>}
      <div className="list">
        {shelf.items.map((item, i) =>
          isPlayable(item) ? (
            <SongRow key={`${item.videoId}-${i}`} item={item} player={player} go={go} onPlay={() => player.playRadio(ytTrack(item))} />
          ) : (
            <button key={`${item.browseId}-${i}`} className="track" onClick={() => item.browseId && go({ name: 'browse', id: item.browseId })}>
              <Thumb src={item.thumbnail} round={item.kind === 'artist'} size={44} />
              <span className="row-text">
                <span className="row-title">{item.title}</span>
                <span className="row-sub">{[item.kind[0].toUpperCase() + item.kind.slice(1), item.subtitle].filter(Boolean).join(' • ')}</span>
              </span>
            </button>
          ),
        )}
      </div>
      {songs.length === 0 && null}
    </section>
  );
}

export function YtBrowse({ id, params, player, go }: { id: string; params?: string | null; player: Player; go: Go }) {
  const { page, error, retry } = usePage(() => api.ytBrowse(id, params), [id, params]);
  const h = page?.header;
  const isArtist = id.startsWith('UC');
  // The page's own track list: the first untitled song shelf (album / playlist).
  const trackShelf = useMemo(() => page?.shelves.find((s) => s.items.length && s.items.every(isPlayable) && (!s.title || !isArtist)), [page, isArtist]);
  const fallback = useMemo(
    () => ({
      album: !isArtist ? h?.title : undefined,
      albumId: !isArtist && id.startsWith('MPRE') ? id : null,
      artist: h?.artists.map((a) => a.name).join(', ') || undefined,
      thumbnail: id.startsWith('MPRE') ? h?.thumbnail ?? null : null,
    }),
    [h, id, isArtist],
  );
  const tracks = useMemo(() => (trackShelf ? trackShelf.items.map((i) => ytTrack(i, fallback)) : []), [trackShelf, fallback]);
  const firstSongs = useMemo(() => page?.shelves.flatMap((s) => s.items.filter(isPlayable)) ?? [], [page]);

  if (error) return <Failed error={error} retry={retry} />;
  if (!page) return <Loading />;

  const playAll = (shuffleStart = false) => {
    if (tracks.length) return player.playList(tracks, shuffleStart ? Math.floor(Math.random() * tracks.length) : 0);
    if (firstSongs.length) player.playRadio(ytTrack(firstSongs[0]));
  };

  return (
    <>
      {h && (
        <div className={`album-head${isArtist ? ' artist' : ''}`}>
          <span
            className={`album-art${isArtist ? ' round' : ''}`}
            style={{ backgroundImage: h.thumbnail ? `url("${h.thumbnail}")` : placeholderArt(h.title), borderRadius: isArtist ? '50%' : 20 }}
          />
          <div className="album-meta">
            {h.subtitle && <div className="date">{h.subtitle}</div>}
            <h1>{h.title}</h1>
            {h.artists.length > 0 && !isArtist && (
              <div className="row-sub big">
                {h.artists.map((a, i) => (
                  <span key={i}>
                    {i > 0 && ', '}
                    {a.id ? (
                      <button className="link" onClick={() => go({ name: 'browse', id: a.id! })}>{a.name}</button>
                    ) : (
                      a.name
                    )}
                  </span>
                ))}
              </div>
            )}
            {h.secondSubtitle && <div className="row-sub big">{h.secondSubtitle}</div>}
            {h.description && <p className="description">{h.description}</p>}
            <div className="album-actions">
              <GlassButton variant="filled" tint={ACCENT} icon={<I.PlayIcon />} onClick={() => playAll()} disabled={!tracks.length && !firstSongs.length}>
                Play
              </GlassButton>
              {tracks.length > 1 && (
                <GlassButton icon={<I.ShuffleIcon />} onClick={() => playAll(true)}>Shuffle</GlassButton>
              )}
              {firstSongs.length > 0 && (
                <GlassButton icon={<I.RadioIcon />} onClick={() => player.playRadio(ytTrack(firstSongs[0], fallback))}>Radio</GlassButton>
              )}
            </div>
          </div>
        </div>
      )}
      {trackShelf && (
        <div className="list">
          {trackShelf.items.map((item, i) => (
            <SongRow
              key={`${item.videoId}-${i}`}
              item={item}
              n={id.startsWith('MPRE') ? i + 1 : undefined}
              fallbackThumb={fallback.thumbnail}
              player={player}
              go={go}
              onPlay={() => player.playList(tracks, i)}
            />
          ))}
        </div>
      )}
      {page.shelves
        .filter((s) => s !== trackShelf)
        .map((s, i) => (
          <ShelfView key={`${s.title}-${i}`} shelf={s} player={player} go={go} />
        ))}
    </>
  );
}
