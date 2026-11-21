import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  GlassButton,
  GlassCard,
  GlassInput,
  GlassMenu,
  GlassSegmented,
  GlassSheet,
  GlassSlider,
  GlassSwitch,
} from '@zakisheriff/liquid-glass';
import * as I from './icons';
import { albumKey, api, fmt, isHiRes, isTauri, kHz, on, pickFolder, qualityLabel, windowAction, type Library, type Track } from './api';
import { Art, artBackground, useCover } from './covers';
import { usePlayer, type Player } from './player';

type Theme = 'system' | 'light' | 'dark';
type View = 'home' | 'songs' | 'albums' | 'album' | 'folders' | 'youtube';
const ACCENT = '#7C4DFF';

interface Album {
  key: string;
  title: string;
  artist: string;
  year: number | null;
  tracks: Track[];
}

const NAV: Array<{ key: View; label: string; icon: JSX.Element }> = [
  { key: 'home', label: 'Home', icon: <I.HomeIcon /> },
  { key: 'songs', label: 'Songs', icon: <I.SongsIcon /> },
  { key: 'albums', label: 'Albums', icon: <I.AlbumIcon /> },
  { key: 'folders', label: 'Folders', icon: <I.FolderIcon /> },
  { key: 'youtube', label: 'YouTube Music', icon: <I.YouTubeIcon /> },
];

function load<T>(key: string, fallback: T): T {
  try {
    const v = localStorage.getItem(key);
    return v === null ? fallback : (JSON.parse(v) as T);
  } catch {
    return fallback;
  }
}
function store(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* storage unavailable */
  }
}

function useResolvedTheme(theme: Theme): 'light' | 'dark' {
  const [systemDark, setSystemDark] = useState(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSystemDark(e.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'system') root.removeAttribute('data-theme');
    else root.setAttribute('data-theme', theme);
  }, [theme]);
  return theme === 'system' ? (systemDark ? 'dark' : 'light') : theme;
}

function greeting() {
  const h = new Date().getHours();
  return h < 5 ? 'Good night' : h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
}

export default function App() {
  const [theme, setTheme] = useState<Theme>(() => {
    const q = new URLSearchParams(location.search).get('theme');
    return q === 'light' || q === 'dark' ? q : load<Theme>('cw.theme', 'system');
  });
  const resolved = useResolvedTheme(theme);
  const [library, setLibrary] = useState<Library>({ folders: [], tracks: [] });
  const [loaded, setLoaded] = useState(false);
  const [scanning, setScanning] = useState<number | null>(null);
  const [view, setView] = useState<View>('home');
  const [openAlbum, setOpenAlbum] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [closeToTray, setCloseToTray] = useState(() => load('cw.tray', true));
  const searchRef = useRef<HTMLInputElement>(null);
  const mainRef = useRef<HTMLElement>(null);

  const byPath = useMemo(() => new Map(library.tracks.map((t) => [t.path, t])), [library.tracks]);
  const player = usePlayer(byPath);

  const albums = useMemo(() => {
    const map = new Map<string, Album>();
    for (const t of library.tracks) {
      const key = albumKey(t);
      let a = map.get(key);
      if (!a) {
        a = { key, title: t.album, artist: t.albumArtist || t.artist, year: t.year, tracks: [] };
        map.set(key, a);
      }
      a.tracks.push(t);
    }
    return [...map.values()].sort((a, b) => a.title.localeCompare(b.title));
  }, [library.tracks]);

  useEffect(() => {
    api.library().then((lib) => {
      setLibrary(lib);
      setLoaded(true);
    });
    return on<number>('library://progress', (n) => setScanning(n));
  }, []);

  useEffect(() => store('cw.theme', theme), [theme]);
  useEffect(() => {
    store('cw.tray', closeToTray);
    api.setCloseToTray(closeToTray);
  }, [closeToTray]);

  const runScan = useCallback(async (job: () => Promise<Library>) => {
    setScanning(0);
    try {
      setLibrary(await job());
    } finally {
      setScanning(null);
    }
  }, []);

  const addFolder = useCallback(async () => {
    const folder = await pickFolder();
    if (folder) {
      setView('folders');
      await runScan(() => api.addFolder(folder));
    }
  }, [runScan]);

  // Keyboard: Space play/pause, Ctrl+K search, Ctrl+Left/Right previous/next.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const typing = (e.target as HTMLElement)?.closest('input, textarea, [contenteditable="true"]');
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        searchRef.current?.focus();
      } else if (typing) {
        return;
      } else if (e.code === 'Space' && !(e.target as HTMLElement)?.closest('button, [role="slider"]')) {
        e.preventDefault();
        player.toggle();
      } else if (e.ctrlKey && e.key === 'ArrowRight') {
        player.next();
      } else if (e.ctrlKey && e.key === 'ArrowLeft') {
        player.previous();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [player]);

  const go = (v: View) => {
    setView(v);
    setQuery('');
    mainRef.current?.scrollTo({ top: 0 });
  };
  const showAlbum = (key: string) => {
    setOpenAlbum(key);
    go('album');
  };

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return null;
    return library.tracks.filter((t) => `${t.title} ${t.artist} ${t.album}`.toLowerCase().includes(q));
  }, [query, library.tracks]);

  const nowCover = useCover(player.current);
  const album = albums.find((a) => a.key === openAlbum) ?? null;
  const empty = loaded && library.tracks.length === 0;

  return (
    <div className="app" data-resolved={resolved}>
      <div className="ambient" aria-hidden="true" style={{ background: artBackground(player.current, nowCover) }} />
      <div className="scrim" aria-hidden="true" />

      <header className="titlebar" data-tauri-drag-region>
        <div className="brand" data-tauri-drag-region>
          <img className="brand-mark" src="/logo.png" alt="" width={22} height={22} />
          <span data-tauri-drag-region>ClaudeWaves</span>
        </div>
        <div className="search">
          <GlassInput
            ref={searchRef}
            type="search"
            placeholder="Search your library   Ctrl+K"
            aria-label="Search your library"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onClear={() => setQuery('')}
            tint={ACCENT}
          />
        </div>
        <div className="title-actions">
          <GlassButton
            size="sm"
            icon={resolved === 'dark' ? <I.SunIcon /> : <I.MoonIcon />}
            aria-label={resolved === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            onClick={() => setTheme(resolved === 'dark' ? 'light' : 'dark')}
          />
          <GlassButton size="sm" icon={<I.SettingsIcon />} aria-label="Settings" onClick={() => setSettingsOpen(true)} />
        </div>
        <div className="win-controls">
          <button className="win-btn" aria-label="Minimize" onClick={() => windowAction('minimize')}><I.WinMin /></button>
          <button className="win-btn" aria-label="Maximize" onClick={() => windowAction('maximize')}><I.WinMax /></button>
          <button className="win-btn win-close" aria-label={closeToTray ? 'Close to tray' : 'Close'} onClick={() => windowAction('close')}><I.WinClose /></button>
        </div>
      </header>

      <div className="body">
        <GlassCard className="sidebar" padding="12px 10px" radius="24px">
          <nav aria-label="Main" className="nav">
            {NAV.map((n) => {
              const active = view === n.key || (n.key === 'albums' && view === 'album');
              return active && !results ? (
                <GlassButton key={n.key} icon={n.icon} className="nav-item" aria-current="page" onClick={() => go(n.key)}>
                  {n.label}
                </GlassButton>
              ) : (
                <button key={n.key} className="row nav-plain" onClick={() => go(n.key)}>
                  {n.icon}
                  <span>{n.label}</span>
                </button>
              );
            })}
          </nav>
          <div className="divider" />
          <div className="lib-stats">
            <span className="eyebrow">Library</span>
            <span className="row-sub">
              {library.tracks.length} songs · {albums.length} albums
            </span>
          </div>
          <div className="spacer" />
          <OutputCard player={player} />
        </GlassCard>

        <main className="content" ref={mainRef}>
          {results ? (
            <SearchView query={query} results={results} player={player} />
          ) : empty && view !== 'youtube' ? (
            <EmptyLibrary onAdd={addFolder} scanning={scanning} />
          ) : view === 'home' ? (
            <HomeView library={library} albums={albums} player={player} onAlbum={showAlbum} />
          ) : view === 'songs' ? (
            <SongsView tracks={library.tracks} player={player} />
          ) : view === 'albums' ? (
            <AlbumsView albums={albums} onAlbum={showAlbum} />
          ) : view === 'album' && album ? (
            <AlbumView album={album} player={player} onBack={() => go('albums')} />
          ) : view === 'folders' ? (
            <FoldersView
              folders={library.folders}
              count={library.tracks.length}
              scanning={scanning}
              onAdd={addFolder}
              onRemove={(f) => runScan(() => api.removeFolder(f))}
              onRescan={() => runScan(api.rescan)}
            />
          ) : (
            <YouTubeView />
          )}
          <div className="player-clearance" />
        </main>

        <NowPlayingPanel player={player} cover={nowCover} />
        <PlayerBar player={player} resolved={resolved} />
      </div>

      {player.error && (
        <div className="toast glass-toast" role="alert">
          <I.AlertIcon />
          <span>{player.error}</span>
          <GlassButton variant="clear" size="sm" onClick={player.clearError}>Dismiss</GlassButton>
        </div>
      )}

      <SettingsSheet
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        theme={theme}
        setTheme={setTheme}
        closeToTray={closeToTray}
        setCloseToTray={setCloseToTray}
      />
    </div>
  );
}

// ---------------------------------------------------------------------------
// Views
// ---------------------------------------------------------------------------

function TrackRow({ t, n, onPlay, active, showAlbum = true }: { t: Track; n?: number; onPlay: () => void; active: boolean; showAlbum?: boolean }) {
  return (
    <button className={`track${active ? ' is-playing' : ''}`} onClick={onPlay} aria-current={active ? 'true' : undefined}>
      {n !== undefined && <span className="track-n">{n}</span>}
      <Art track={t} size={40} radius={8} />
      <span className="row-text">
        <span className="row-title">{t.title}</span>
        <span className="row-sub">{t.artist}</span>
      </span>
      {showAlbum && <span className="row-sub track-album">{t.album}</span>}
      <span className="track-q">{qualityLabel(t)}</span>
      <span className="row-sub track-dur">{fmt(t.duration)}</span>
    </button>
  );
}

function HomeView({ library, albums, player, onAlbum }: { library: Library; albums: Album[]; player: Player; onAlbum: (k: string) => void }) {
  const recent = useMemo(() => [...library.tracks].sort((a, b) => b.added - a.added).slice(0, 6), [library.tracks]);
  const hires = useMemo(() => library.tracks.filter(isHiRes), [library.tracks]);
  const recentAlbums = useMemo(
    () => [...albums].sort((a, b) => Math.max(...b.tracks.map((t) => t.added)) - Math.max(...a.tracks.map((t) => t.added))).slice(0, 10),
    [albums],
  );
  const now = new Date();
  return (
    <>
      <div className="content-head">
        <div>
          <div className="date">{now.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })}</div>
          <h1>{greeting()}</h1>
        </div>
        <GlassButton variant="tinted" tint={ACCENT} icon={<I.ShuffleIcon />} onClick={() => player.playList(library.tracks, Math.floor(Math.random() * library.tracks.length))}>
          Shuffle all
        </GlassButton>
      </div>

      <section aria-labelledby="recent">
        <h2 id="recent">Recently added</h2>
        <div className="quick-grid">
          {recent.map((t, i) => (
            <button key={t.path} className="quick" onClick={() => player.playList(recent, i)}>
              <Art track={t} size={52} radius={11} />
              <span className="row-text">
                <span className="row-title">{t.title}</span>
                <span className="row-sub">{t.artist}</span>
              </span>
              <span className="row-sub">{fmt(t.duration)}</span>
            </button>
          ))}
        </div>
      </section>

      <section aria-labelledby="albums-h">
        <h2 id="albums-h">Albums</h2>
        <AlbumGrid albums={recentAlbums} onAlbum={onAlbum} />
      </section>

      {hires.length > 0 && (
        <section aria-labelledby="hires">
          <div className="section-row">
            <h2 id="hires">Hi-Res in your library</h2>
            <GlassButton variant="clear" size="sm" onClick={() => player.playList(hires)}>Play all</GlassButton>
          </div>
          <div className="list">
            {hires.slice(0, 8).map((t, i) => (
              <TrackRow key={t.path} t={t} active={player.state.path === t.path} onPlay={() => player.playList(hires, i)} />
            ))}
          </div>
        </section>
      )}
    </>
  );
}

function SongsView({ tracks, player }: { tracks: Track[]; player: Player }) {
  const [sort, setSort] = useState(0);
  const sorted = useMemo(() => {
    const a = [...tracks];
    if (sort === 0) a.sort((x, y) => x.title.localeCompare(y.title));
    else if (sort === 1) a.sort((x, y) => x.artist.localeCompare(y.artist) || x.album.localeCompare(y.album) || (x.trackNo ?? 0) - (y.trackNo ?? 0));
    else a.sort((x, y) => y.added - x.added);
    return a;
  }, [tracks, sort]);
  return (
    <>
      <div className="content-head">
        <div>
          <div className="date">{tracks.length} songs</div>
          <h1>Songs</h1>
        </div>
        <GlassSegmented aria-label="Sort" options={['Title', 'Artist', 'Recent']} value={sort} onChange={setSort} />
      </div>
      <div className="list">
        {sorted.map((t, i) => (
          <TrackRow key={t.path} t={t} active={player.state.path === t.path} onPlay={() => player.playList(sorted, i)} />
        ))}
      </div>
    </>
  );
}

function AlbumGrid({ albums, onAlbum }: { albums: Album[]; onAlbum: (k: string) => void }) {
  return (
    <div className="mix-grid">
      {albums.map((a) => (
        <button key={a.key} className="mix" onClick={() => onAlbum(a.key)}>
          <Art track={a.tracks.find((t) => t.hasCover) ?? a.tracks[0]} className="mix-art" radius={18} />
          <span className="row-title">{a.title}</span>
          <span className="row-sub">{a.artist}</span>
        </button>
      ))}
    </div>
  );
}

function AlbumsView({ albums, onAlbum }: { albums: Album[]; onAlbum: (k: string) => void }) {
  return (
    <>
      <div className="content-head">
        <div>
          <div className="date">{albums.length} albums</div>
          <h1>Albums</h1>
        </div>
      </div>
      <AlbumGrid albums={albums} onAlbum={onAlbum} />
    </>
  );
}

function AlbumView({ album, player, onBack }: { album: Album; player: Player; onBack: () => void }) {
  const tracks = useMemo(
    () => [...album.tracks].sort((a, b) => (a.discNo ?? 1) - (b.discNo ?? 1) || (a.trackNo ?? 0) - (b.trackNo ?? 0)),
    [album],
  );
  const total = tracks.reduce((s, t) => s + t.duration, 0);
  return (
    <>
      <GlassButton variant="clear" size="sm" icon={<I.BackIcon />} onClick={onBack} className="back">Albums</GlassButton>
      <div className="album-head">
        <Art track={tracks.find((t) => t.hasCover) ?? tracks[0]} className="album-art" radius={20} />
        <div className="album-meta">
          <div className="date">Album{album.year ? ` · ${album.year}` : ''}</div>
          <h1>{album.title}</h1>
          <div className="row-sub big">{album.artist} · {tracks.length} songs · {fmt(total)}</div>
          <div className="album-actions">
            <GlassButton variant="filled" tint={ACCENT} icon={<I.PlayIcon />} onClick={() => player.playList(tracks)}>Play</GlassButton>
            <GlassButton icon={<I.ShuffleIcon />} onClick={() => player.playList(tracks, Math.floor(Math.random() * tracks.length))}>Shuffle</GlassButton>
          </div>
        </div>
      </div>
      <div className="list">
        {tracks.map((t, i) => (
          <TrackRow key={t.path} t={t} n={t.trackNo ?? i + 1} showAlbum={false} active={player.state.path === t.path} onPlay={() => player.playList(tracks, i)} />
        ))}
      </div>
    </>
  );
}

function SearchView({ query, results, player }: { query: string; results: Track[]; player: Player }) {
  return (
    <>
      <div className="content-head">
        <div>
          <div className="date">{results.length} results</div>
          <h1>“{query}”</h1>
        </div>
      </div>
      {results.length === 0 ? (
        <p className="row-sub">Nothing in your library matches that. Search covers titles, artists and albums.</p>
      ) : (
        <div className="list">
          {results.slice(0, 300).map((t, i) => (
            <TrackRow key={t.path} t={t} active={player.state.path === t.path} onPlay={() => player.playList(results, i)} />
          ))}
        </div>
      )}
    </>
  );
}

function FoldersView(props: {
  folders: string[];
  count: number;
  scanning: number | null;
  onAdd: () => void;
  onRemove: (f: string) => void;
  onRescan: () => void;
}) {
  return (
    <>
      <div className="content-head">
        <div>
          <div className="date">{props.scanning !== null ? `Scanning… ${props.scanning} files` : `${props.count} songs`}</div>
          <h1>Folders</h1>
        </div>
        <div className="album-actions">
          <GlassButton icon={<I.RefreshIcon />} onClick={props.onRescan} disabled={props.scanning !== null || !props.folders.length}>Rescan</GlassButton>
          <GlassButton variant="filled" tint={ACCENT} icon={<I.PlusIcon />} onClick={props.onAdd} disabled={props.scanning !== null || !isTauri}>Add folder</GlassButton>
        </div>
      </div>
      <GlassCard padding="8px" radius="20px" className="folder-card">
        {props.folders.length === 0 ? (
          <p className="row-sub pad">No folders yet. Add the folder where you keep your music.</p>
        ) : (
          <ul className="folders">
            {props.folders.map((f) => (
              <li key={f}>
                <I.FolderIcon />
                <span className="row-title folder-path" title={f}>{f}</span>
                <GlassButton variant="clear" size="sm" destructive icon={<I.TrashIcon />} aria-label={`Remove ${f}`} onClick={() => props.onRemove(f)} disabled={props.scanning !== null} />
              </li>
            ))}
          </ul>
        )}
      </GlassCard>
      <p className="row-sub note">Reads FLAC, MP3, WAV, OGG Vorbis, M4A (AAC and ALAC) and AIFF. Covers come from the file’s tags or a cover.jpg / folder.jpg next to it.</p>
    </>
  );
}

function EmptyLibrary({ onAdd, scanning }: { onAdd: () => void; scanning: number | null }) {
  return (
    <div className="empty">
      <GlassCard padding="32px" radius="28px" className="empty-card">
        <span className="empty-icon"><I.FolderIcon /></span>
        <h1>Add your music</h1>
        <p className="row-sub big">Pick the folder where your songs live. ClaudeWaves reads the tags and covers and keeps the library up to date when you rescan.</p>
        <GlassButton variant="filled" tint={ACCENT} size="lg" icon={<I.PlusIcon />} onClick={onAdd} disabled={scanning !== null || !isTauri}>
          {scanning !== null ? `Scanning… ${scanning}` : 'Choose folder'}
        </GlassButton>
        {!isTauri && <p className="row-sub">Folder access needs the desktop app.</p>}
      </GlassCard>
    </div>
  );
}

function YouTubeView() {
  return (
    <div className="empty">
      <GlassCard padding="32px" radius="28px" className="empty-card">
        <span className="empty-icon"><I.YouTubeIcon /></span>
        <h1>YouTube Music</h1>
        <p className="row-sub big">Streaming from YouTube Music, with Best Part jumps, comes in a later update. This version plays your local files.</p>
      </GlassCard>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Panels
// ---------------------------------------------------------------------------

function OutputCard({ player }: { player: Player }) {
  const o = player.output;
  return (
    <GlassCard className="device" padding="12px" radius="16px" variant="clear">
      <span className="device-icon" style={{ color: ACCENT }}><I.DacIcon /></span>
      <span className="row-text">
        <span className="row-title" title={o?.device}>{o?.device || 'Audio output'}</span>
        <span className="row-sub">{o?.sampleRate ? `${o.mode} · ${kHz(o.sampleRate)} kHz` : o ? 'Not available' : 'Starting…'}</span>
      </span>
      <span className={`dot${o?.sampleRate ? '' : ' off'}`} />
    </GlassCard>
  );
}

function NowPlayingPanel({ player, cover }: { player: Player; cover: string | null }) {
  const t = player.current;
  const o = player.output;
  return (
    <GlassCard className="aside" padding="14px" radius="24px">
      <div className="now-art" style={{ background: artBackground(t, cover) }}>
        {t && (
          <div className="badges">
            <span className="badge">{qualityLabel(t)}</span>
            {isHiRes(t) && <span className="badge">Hi-Res</span>}
          </div>
        )}
      </div>
      <div className="now-meta">
        <div className="now-title">{t?.title ?? 'Nothing playing'}</div>
        <div className="row-sub">{t ? `${t.artist} · ${t.album}` : 'Pick a song to start'}</div>
        {t && o?.sampleRate ? (
          <div className="signal">
            {t.sampleRate ? `${kHz(t.sampleRate)} kHz` : t.format} → {o.resampling ? `resampled to ${kHz(o.sampleRate)} kHz` : `${kHz(o.sampleRate)} kHz, no resampling`}
          </div>
        ) : null}
      </div>
      <div className="section-head">
        <span className="eyebrow">Up next</span>
        <span className="row-sub">{player.upNext.length} songs</span>
      </div>
      <ol className="queue">
        {player.upNext.slice(0, 50).map((u, i) => (
          <li key={`${u.path}-${i}`}>
            <button className="row" onClick={() => player.playFromQueue(player.queueStart + i)}>
              <Art track={u} size={40} radius={9} />
              <span className="row-text">
                <span className="row-title">{u.title}</span>
                <span className="row-sub">{u.artist}</span>
              </span>
              <span className="row-sub">{fmt(u.duration)}</span>
            </button>
          </li>
        ))}
        {player.upNext.length === 0 && <li className="row-sub pad">The queue is empty.</li>}
      </ol>
    </GlassCard>
  );
}

function PlayerBar({ player, resolved }: { player: Player; resolved: 'light' | 'dark' }) {
  const { state, current: t } = player;
  // While dragging, show the thumb where the finger is; seek after it settles.
  const [drag, setDrag] = useState<number | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout>>();
  const pos = drag ?? state.position;
  const onSeek = (v: number) => {
    setDrag(v);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      player.seek(v);
      setTimeout(() => setDrag(null), 300);
    }, 120);
  };
  const duration = state.duration || t?.duration || 0;

  return (
    <GlassCard className="player" padding="10px 16px 10px 10px" radius="28px">
      <div className="player-left">
        <Art track={t} size={54} radius={12} />
        <span className="row-text">
          <span className="row-title">{t?.title ?? 'Not playing'}</span>
          <span className="row-sub">{t?.artist ?? '—'}</span>
        </span>
      </div>

      <div className="player-center">
        <div className="transport">
          <GlassButton
            variant="clear"
            size="sm"
            icon={<I.ShuffleIcon />}
            aria-label="Shuffle"
            aria-pressed={player.shuffle}
            className={player.shuffle ? 'is-on' : ''}
            onClick={player.toggleShuffle}
          />
          <GlassButton variant="clear" icon={<I.PrevIcon />} aria-label="Previous" onClick={player.previous} />
          <GlassButton size="lg" icon={state.playing ? <I.PauseIcon /> : <I.PlayIcon />} aria-label={state.playing ? 'Pause' : 'Play'} onClick={player.toggle} />
          <GlassButton variant="clear" icon={<I.NextIcon />} aria-label="Next" onClick={player.next} />
          <GlassButton
            variant="clear"
            size="sm"
            icon={player.repeat === 'one' ? <I.RepeatOneIcon /> : <I.RepeatIcon />}
            aria-label={`Repeat: ${player.repeat}`}
            aria-pressed={player.repeat !== 'off'}
            className={player.repeat !== 'off' ? 'is-on' : ''}
            onClick={player.cycleRepeat}
          />
        </div>
        <div className="seek">
          <span className="time right">{fmt(pos)}</span>
          <div className="seek-track">
            <GlassSlider
              aria-label="Seek"
              min={0}
              max={Math.max(duration, 1)}
              value={Math.min(pos, duration)}
              onChange={onSeek}
              disabled={!t}
              tint={resolved === 'dark' ? '#ffffff' : '#1c1c1e'}
            />
          </div>
          <span className="time">{fmt(duration)}</span>
        </div>
      </div>

      <div className="player-right">
        {t && <span className="chip" style={{ color: "var(--cw-accent-text)", borderColor: "currentColor" }}>{qualityLabel(t)}</span>}
        <GlassMenu
          aria-label="More"
          size="sm"
          icon={<I.MoreIcon />}
          align="end"
          items={[
            { label: 'Shuffle', icon: <I.ShuffleIcon />, checked: player.shuffle, onSelect: player.toggleShuffle },
            { label: `Repeat: ${player.repeat}`, icon: <I.RepeatIcon />, onSelect: player.cycleRepeat },
          ]}
        />
        <span className="vol-icon"><I.VolumeIcon /></span>
        <div className="vol">
          <GlassSlider aria-label="Volume" min={0} max={1} step="any" value={player.volume} onChange={player.setVolume} tint={ACCENT} />
        </div>
      </div>
    </GlassCard>
  );
}

function SettingsSheet(props: {
  open: boolean;
  onClose: () => void;
  theme: Theme;
  setTheme: (t: Theme) => void;
  closeToTray: boolean;
  setCloseToTray: (v: boolean) => void;
}) {
  const themes: Theme[] = ['system', 'light', 'dark'];
  const [devices, setDevices] = useState<string[]>([]);
  const [device, setDevice] = useState<string | null>(() => load<string | null>('cw.device', null));

  useEffect(() => {
    if (props.open) api.outputDevices().then(setDevices);
  }, [props.open]);
  useEffect(() => {
    if (device) api.setOutputDevice(device);
    // Only restore the saved device once at start-up.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const pick = (name: string | null) => {
    setDevice(name);
    store('cw.device', name);
    api.setOutputDevice(name);
  };

  return (
    <GlassSheet open={props.open} onClose={props.onClose} aria-label="Settings">
      <div className="sheet">
        <div className="sheet-head">
          <h2>Settings</h2>
          <GlassButton size="sm" onClick={props.onClose}>Done</GlassButton>
        </div>
        <div className="setting">
          <span className="row-text">
            <span className="row-title">Appearance</span>
            <span className="row-sub">Follow Windows, or pick one</span>
          </span>
          <GlassSegmented aria-label="Appearance" size="sm" options={['System', 'Light', 'Dark']} value={themes.indexOf(props.theme)} onChange={(i) => props.setTheme(themes[i])} />
        </div>
        <div className="setting column">
          <span className="row-text">
            <span className="row-title">Output device</span>
            <span className="row-sub">Where ClaudeWaves plays. “System default” follows the Windows sound settings.</span>
          </span>
          <div className="device-list" role="radiogroup" aria-label="Output device">
            {[null, ...devices].map((d) => (
              <button key={d ?? 'default'} role="radio" aria-checked={device === d} className={`row device-row${device === d ? ' is-on' : ''}`} onClick={() => pick(d)}>
                <span className="row-title">{d ?? 'System default'}</span>
                {device === d && <I.CheckIcon />}
              </button>
            ))}
          </div>
        </div>
        <div className="setting">
          <span className="row-text">
            <span className="row-title">Keep playing in tray</span>
            <span className="row-sub">Closing the window hides it to the system tray. Quit from the tray icon.</span>
          </span>
          <GlassSwitch checked={props.closeToTray} onChange={props.setCloseToTray} tint={ACCENT} aria-label="Keep playing in tray" />
        </div>
        <div className="setting">
          <span className="row-text">
            <span className="row-title">Exclusive mode (bit-perfect)</span>
            <span className="row-sub">WASAPI Exclusive output for USB DACs. Coming in the next update.</span>
          </span>
          <GlassSwitch checked={false} disabled tint={ACCENT} aria-label="Exclusive mode" />
        </div>
      </div>
    </GlassSheet>
  );
}
