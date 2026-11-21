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
import { albumKey, api, fmt, isTauri, kHz, on, pickFolder, qualityLabel, windowAction, type Library, type YtDlpStatus } from './api';
import { Art, artBackground, useCover } from './covers';
import { usePlayer, type Player } from './player';
import { AlbumView, AlbumsView, EmptyLibrary, FoldersView, LocalSearch, SongsView, type Album } from './local';
import { YtBrowse, YtHome, YtSearch, type Route } from './yt';
import { LyricsView } from './lyrics';

type Theme = 'system' | 'light' | 'dark';
const ACCENT = '#7C4DFF';

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

/** Back / forward history of screens, like a browser. */
function useHistory() {
  const [stack, setStack] = useState<Route[]>([{ name: 'home' }]);
  const [at, setAt] = useState(0);
  const go = useCallback(
    (r: Route) => {
      setStack((s) => [...s.slice(0, at + 1), r]);
      setAt((a) => a + 1);
    },
    [at],
  );
  return {
    route: stack[at],
    go,
    back: () => setAt((a) => Math.max(0, a - 1)),
    forward: () => setAt((a) => Math.min(stack.length - 1, a + 1)),
    canBack: at > 0,
    canForward: at < stack.length - 1,
  };
}

const NAV: Array<{ route: Route; label: string; icon: JSX.Element; match: Route['name'][] }> = [
  { route: { name: 'home' }, label: 'Home', icon: <I.HomeIcon />, match: ['home'] },
  { route: { name: 'browse', id: 'FEmusic_explore' }, label: 'Explore', icon: <I.ExploreIcon />, match: [] },
  { route: { name: 'browse', id: 'FEmusic_charts' }, label: 'Charts', icon: <I.ChartIcon />, match: [] },
];
const LIB_NAV: Array<{ route: Route; label: string; icon: JSX.Element; match: Route['name'][] }> = [
  { route: { name: 'songs' }, label: 'Songs', icon: <I.SongsIcon />, match: ['songs'] },
  { route: { name: 'albums' }, label: 'Albums', icon: <I.AlbumIcon />, match: ['albums', 'album'] },
  { route: { name: 'folders' }, label: 'Folders', icon: <I.FolderIcon />, match: ['folders'] },
];

export default function App() {
  const [theme, setTheme] = useState<Theme>(() => {
    const q = new URLSearchParams(location.search).get('theme');
    return q === 'light' || q === 'dark' ? q : load<Theme>('cw.theme', 'system');
  });
  const resolved = useResolvedTheme(theme);
  const [library, setLibrary] = useState<Library>({ folders: [], tracks: [] });
  const [scanning, setScanning] = useState<number | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [closeToTray, setCloseToTray] = useState(() => load('cw.tray', true));
  const [aside, setAside] = useState(() => load('cw.aside', 0));
  const nav = useHistory();
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
    api.library().then(setLibrary);
    return on<number>('library://progress', (n) => setScanning(n));
  }, []);
  useEffect(() => store('cw.theme', theme), [theme]);
  useEffect(() => store('cw.aside', aside), [aside]);
  useEffect(() => {
    store('cw.tray', closeToTray);
    api.setCloseToTray(closeToTray);
  }, [closeToTray]);
  useEffect(() => mainRef.current?.scrollTo({ top: 0 }), [nav.route]);

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
    if (folder) await runScan(() => api.addFolder(folder));
  }, [runScan]);

  // Keyboard: Space play/pause, Ctrl+K search, Ctrl+Left/Right previous/next, Alt+Left back.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      const typing = target?.closest('input, textarea, [contenteditable="true"]');
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        document.querySelector<HTMLInputElement>('.search input')?.focus();
      } else if (typing) {
        return;
      } else if (e.code === 'Space' && !target?.closest('button, [role="slider"]')) {
        e.preventDefault();
        player.toggle();
      } else if (e.ctrlKey && e.key === 'ArrowRight') {
        player.next();
      } else if (e.ctrlKey && e.key === 'ArrowLeft') {
        player.previous();
      } else if (e.altKey && e.key === 'ArrowLeft') {
        nav.back();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [player, nav]);

  const nowCover = useCover(player.current);
  const r = nav.route;
  const isActive = (n: (typeof NAV)[number]) =>
    n.match.includes(r.name) || (n.route.name === 'browse' && r.name === 'browse' && (n.route as { id: string }).id === r.id);

  let content: JSX.Element;
  if (r.name === 'home') content = <YtHome player={player} go={nav.go} />;
  else if (r.name === 'search') content = <SearchScreen q={r.q} library={library} player={player} go={nav.go} />;
  else if (r.name === 'browse') content = <YtBrowse key={r.id} id={r.id} params={r.params} player={player} go={nav.go} />;
  else if (r.name === 'folders' || library.tracks.length === 0)
    content =
      r.name === 'folders' ? (
        <FoldersView
          folders={library.folders}
          count={library.tracks.length}
          scanning={scanning}
          onAdd={addFolder}
          onRemove={(f) => runScan(() => api.removeFolder(f))}
          onRescan={() => runScan(api.rescan)}
        />
      ) : (
        <EmptyLibrary onAdd={addFolder} scanning={scanning} />
      );
  else if (r.name === 'songs') content = <SongsView tracks={library.tracks} player={player} />;
  else if (r.name === 'albums') content = <AlbumsView albums={albums} onAlbum={(key) => nav.go({ name: 'album', key })} />;
  else {
    const album = albums.find((a) => a.key === r.key);
    content = album ? <AlbumView album={album} player={player} onBack={() => nav.go({ name: 'albums' })} /> : <p className="row-sub">Album not found.</p>;
  }

  return (
    <div className="app" data-resolved={resolved}>
      <div className="ambient" aria-hidden="true" style={{ background: artBackground(player.current, nowCover) }} />
      <div className="scrim" aria-hidden="true" />

      <header className="titlebar" data-tauri-drag-region>
        <div className="brand" data-tauri-drag-region>
          <img className="brand-mark" src="/logo.png" alt="" width={22} height={22} />
          <span data-tauri-drag-region>ClaudeWaves</span>
        </div>
        <div className="history">
          <GlassButton variant="clear" size="sm" icon={<I.BackIcon />} aria-label="Back" onClick={nav.back} disabled={!nav.canBack} />
          <GlassButton variant="clear" size="sm" icon={<I.ForwardIcon />} aria-label="Forward" onClick={nav.forward} disabled={!nav.canForward} />
        </div>
        <SearchBox onSearch={(q) => nav.go({ name: 'search', q })} />
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
            {NAV.map((n) => (
              <NavItem key={n.label} active={isActive(n)} icon={n.icon} label={n.label} onClick={() => nav.go(n.route)} />
            ))}
            <div className="divider" />
            <span className="eyebrow nav-eyebrow">Your library</span>
            {LIB_NAV.map((n) => (
              <NavItem key={n.label} active={isActive(n)} icon={n.icon} label={n.label} onClick={() => nav.go(n.route)} />
            ))}
          </nav>
          <div className="spacer" />
          <OutputCard player={player} />
        </GlassCard>

        <main className="content" ref={mainRef}>
          {content}
          <div className="player-clearance" />
        </main>

        <NowPlayingPanel player={player} cover={nowCover} tab={aside} setTab={setAside} go={nav.go} />
        <PlayerBar player={player} resolved={resolved} />
      </div>

      {player.error && (
        <div className="toast" role="alert">
          <I.AlertIcon />
          <span title={player.error}>{player.error}</span>
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

function NavItem({ active, icon, label, onClick }: { active: boolean; icon: JSX.Element; label: string; onClick: () => void }) {
  return active ? (
    <GlassButton icon={icon} className="nav-item" aria-current="page" onClick={onClick}>
      {label}
    </GlassButton>
  ) : (
    <button className="row nav-plain" onClick={onClick}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

/** Search field with YouTube Music suggestions. */
function SearchBox({ onSearch }: { onSearch: (q: string) => void }) {
  const [q, setQ] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [sel, setSel] = useState(-1);

  useEffect(() => {
    const term = q.trim();
    if (term.length < 2) return setSuggestions([]);
    const id = setTimeout(() => api.ytSuggest(term).then(setSuggestions).catch(() => setSuggestions([])), 180);
    return () => clearTimeout(id);
  }, [q]);

  const submit = (term: string) => {
    const t = term.trim();
    if (!t) return;
    setQ(t);
    setOpen(false);
    setSel(-1);
    onSearch(t);
  };

  return (
    <div
      className="search"
      onKeyDown={(e) => {
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          setOpen(true);
          setSel((s) => Math.min(suggestions.length - 1, s + 1));
        } else if (e.key === 'ArrowUp') {
          e.preventDefault();
          setSel((s) => Math.max(-1, s - 1));
        } else if (e.key === 'Enter') {
          submit(sel >= 0 ? suggestions[sel] : q);
        } else if (e.key === 'Escape') {
          setOpen(false);
        }
      }}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node)) setOpen(false);
      }}
    >
      <GlassInput
        type="search"
        placeholder="Search songs, artists, albums   Ctrl+K"
        aria-label="Search YouTube Music"
        value={q}
        onChange={(e) => {
          setQ(e.target.value);
          setOpen(true);
          setSel(-1);
        }}
        onFocus={() => setOpen(true)}
        onClear={() => setQ('')}
        tint={ACCENT}
      />
      {open && suggestions.length > 0 && (
        <ul className="suggest glass-pop" role="listbox" aria-label="Suggestions">
          {suggestions.map((s, i) => (
            <li key={s} role="option" aria-selected={i === sel}>
              <button className={`suggest-item${i === sel ? ' is-on' : ''}`} tabIndex={-1} onMouseDown={(e) => e.preventDefault()} onClick={() => submit(s)}>
                <I.ExploreIcon />
                <span>{s}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** YouTube Music results, plus matching songs from the local library on top. */
function SearchScreen({ q, library, player, go }: { q: string; library: Library; player: Player; go: (r: Route) => void }) {
  const local = useMemo(() => {
    const term = q.toLowerCase();
    return library.tracks.filter((t) => `${t.title} ${t.artist} ${t.album}`.toLowerCase().includes(term));
  }, [q, library.tracks]);
  return (
    <>
      <YtSearch q={q} player={player} go={go} />
      {local.length > 0 && (
        <div className="local-results">
          <LocalSearch query={q} results={local} player={player} />
        </div>
      )}
    </>
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

function streamLabel(player: Player): string | null {
  const t = player.current;
  if (!t) return null;
  if (t.source !== 'yt') return qualityLabel(t);
  const s = player.stream;
  if (!s) return null;
  return [s.codec || 'AAC', s.bitrate ? `${s.bitrate} kbps` : null].filter(Boolean).join(' ');
}

function NowPlayingPanel({
  player,
  cover,
  tab,
  setTab,
  go,
}: {
  player: Player;
  cover: string | null;
  tab: number;
  setTab: (n: number) => void;
  go: (r: Route) => void;
}) {
  const t = player.current;
  const o = player.output;
  const label = streamLabel(player);
  const sourceRate = t?.source === 'yt' ? player.stream?.sampleRate : t?.sampleRate;
  return (
    <GlassCard className="aside" padding="14px" radius="24px">
      <div className="now-art" style={{ background: artBackground(t, cover) }}>
        {label && (
          <div className="badges">
            <span className="badge">{label}</span>
          </div>
        )}
      </div>
      <div className="now-meta">
        <div className="now-title">{t?.title ?? 'Nothing playing'}</div>
        <div className="row-sub">
          {t ? (
            <>
              {t.artistId ? (
                <button className="link" onClick={() => go({ name: 'browse', id: t.artistId! })}>{t.artist}</button>
              ) : (
                t.artist
              )}
              {t.album && (
                <>
                  {' · '}
                  {t.albumId ? <button className="link" onClick={() => go({ name: 'browse', id: t.albumId! })}>{t.album}</button> : t.album}
                </>
              )}
            </>
          ) : (
            'Search or pick something from Home'
          )}
        </div>
        {t && o?.sampleRate && sourceRate ? (
          <div className="signal">
            {kHz(sourceRate)} kHz → {sourceRate !== o.sampleRate ? `resampled to ${kHz(o.sampleRate)} kHz` : `${kHz(o.sampleRate)} kHz, no resampling`}
          </div>
        ) : null}
      </div>
      <GlassSegmented aria-label="Panel" size="sm" options={['Up next', 'Lyrics']} value={tab} onChange={setTab} />
      {tab === 0 ? (
        <ol className="queue">
          {player.upNext.slice(0, 80).map((u, i) => (
            <li key={`${u.path}-${i}`} className="queue-item">
              <button className="row" onClick={() => player.playFromQueue(player.queueStart + i)}>
                <Art track={u} size={40} radius={9} />
                <span className="row-text">
                  <span className="row-title">{u.title}</span>
                  <span className="row-sub">{u.artist}</span>
                </span>
                {u.duration ? <span className="row-sub">{fmt(u.duration)}</span> : null}
              </button>
              <button className="queue-remove" aria-label={`Remove ${u.title} from queue`} onClick={() => player.removeFromQueue(player.queueStart + i)}>
                <I.WinClose />
              </button>
            </li>
          ))}
          {player.upNext.length === 0 && <li className="row-sub pad">The queue is empty. YouTube songs keep playing with radio when it runs out.</li>}
        </ol>
      ) : (
        <LyricsView player={player} />
      )}
    </GlassCard>
  );
}

function PlayerBar({ player, resolved }: { player: Player; resolved: 'light' | 'dark' }) {
  const { state, current: t } = player;
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
  const label = streamLabel(player);

  return (
    <GlassCard className="player" padding="10px 16px 10px 10px" radius="28px">
      <div className="player-left">
        <Art track={t} size={54} radius={12} />
        <span className="row-text">
          <span className="row-title">{t?.title ?? 'Not playing'}</span>
          <span className="row-sub">{player.buffering ? 'Loading…' : t?.artist ?? '—'}</span>
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
          <GlassButton
            size="lg"
            icon={player.buffering ? <I.SpinnerIcon /> : state.playing ? <I.PauseIcon /> : <I.PlayIcon />}
            aria-label={player.buffering ? 'Loading' : state.playing ? 'Pause' : 'Play'}
            onClick={player.toggle}
          />
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
        {label && <span className="chip" style={{ color: 'var(--cw-accent-text)', borderColor: 'currentColor' }}>{label}</span>}
        <GlassMenu
          aria-label="More"
          size="sm"
          icon={<I.MoreIcon />}
          align="end"
          items={[
            { label: 'Shuffle', icon: <I.ShuffleIcon />, checked: player.shuffle, onSelect: player.toggleShuffle },
            { label: `Repeat: ${player.repeat}`, icon: <I.RepeatIcon />, onSelect: player.cycleRepeat },
            ...(t?.videoId ? [{ label: 'Start radio', icon: <I.RadioIcon />, onSelect: () => player.playRadio(t) }] : []),
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

const REGIONS: Array<[string, string]> = [
  ['MY', 'Malaysia'],
  ['ID', 'Indonesia'],
  ['SG', 'Singapore'],
  ['US', 'United States'],
  ['GB', 'United Kingdom'],
  ['JP', 'Japan'],
  ['KR', 'South Korea'],
  ['IN', 'India'],
];

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
  const [region, setRegion] = useState(() => load('cw.region', 'MY'));
  const [ytdlp, setYtdlp] = useState<YtDlpStatus | null>(null);
  const [installing, setInstalling] = useState(false);
  const [installError, setInstallError] = useState<string | null>(null);

  useEffect(() => {
    if (!props.open) return;
    api.outputDevices().then(setDevices);
    api.ytdlpStatus().then(setYtdlp);
  }, [props.open]);
  useEffect(() => {
    if (device) api.setOutputDevice(device);
    api.ytRegion('en', region);
    // Restore saved choices once at start-up.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const pick = (name: string | null) => {
    setDevice(name);
    store('cw.device', name);
    api.setOutputDevice(name);
  };
  const pickRegion = (gl: string) => {
    setRegion(gl);
    store('cw.region', gl);
    api.ytRegion('en', gl);
  };
  const install = async () => {
    setInstalling(true);
    setInstallError(null);
    try {
      setYtdlp(await api.ytdlpInstall());
    } catch (e) {
      setInstallError(String(e));
    } finally {
      setInstalling(false);
    }
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
        <div className="setting">
          <span className="row-text">
            <span className="row-title">Content region</span>
            <span className="row-sub">Which country’s YouTube Music home, charts and catalogue to show</span>
          </span>
          <select className="select" value={region} onChange={(e) => pickRegion(e.target.value)} aria-label="Content region">
            {REGIONS.map(([code, name]) => (
              <option key={code} value={code}>{name}</option>
            ))}
          </select>
        </div>
        <div className="setting">
          <span className="row-text">
            <span className="row-title">Stream helper (yt-dlp)</span>
            <span className="row-sub">
              {ytdlp === null
                ? 'Checking…'
                : ytdlp.installed && ytdlp.jsRuntime
                  ? `Installed, version ${ytdlp.version}, with Deno. Updates itself every few days.`
                  : ytdlp.installed
                    ? `Version ${ytdlp.version} is installed, but Deno (the JavaScript runtime yt-dlp uses for YouTube) is missing.`
                    : 'Needed to play YouTube Music. Downloads yt-dlp and Deno from their official GitHub releases (about 60 MB).'}
              {installError && <span className="error-text"> {installError}</span>}
            </span>
          </span>
          {ytdlp && !(ytdlp.installed && ytdlp.jsRuntime) && (
            <GlassButton size="sm" variant="filled" tint={ACCENT} onClick={install} disabled={installing || !isTauri}>
              {installing ? 'Installing…' : 'Install'}
            </GlassButton>
          )}
        </div>
        <div className="setting column">
          <span className="row-text">
            <span className="row-title">Output device</span>
            <span className="row-sub">“System default” follows the Windows sound settings.</span>
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
            <span className="row-sub">WASAPI Exclusive output for USB DACs. Coming in a later update.</span>
          </span>
          <GlassSwitch checked={false} disabled tint={ACCENT} aria-label="Exclusive mode" />
        </div>
      </div>
    </GlassSheet>
  );
}
