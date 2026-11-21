import { useEffect, useMemo, useState } from 'react';
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
import { cover, fmt, mixes, nowPlaying, playlists, quickPicks, upNext } from './data';

type Theme = 'system' | 'light' | 'dark';
const ACCENT = '#7C4DFF';

const NAV = [
  { key: 'home', label: 'Home', icon: <I.HomeIcon /> },
  { key: 'explore', label: 'Explore', icon: <I.ExploreIcon /> },
  { key: 'library', label: 'Library', icon: <I.LibraryIcon /> },
  { key: 'local', label: 'Local files', icon: <I.FolderIcon /> },
  { key: 'downloads', label: 'Downloads', icon: <I.DownloadIcon /> },
];

function useResolvedTheme(theme: Theme): 'light' | 'dark' {
  const [systemDark, setSystemDark] = useState(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const on = (e: MediaQueryListEvent) => setSystemDark(e.matches);
    mq.addEventListener('change', on);
    return () => mq.removeEventListener('change', on);
  }, []);
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'system') root.removeAttribute('data-theme');
    else root.setAttribute('data-theme', theme);
  }, [theme]);
  return theme === 'system' ? (systemDark ? 'dark' : 'light') : theme;
}

export default function App() {
  const [theme, setTheme] = useState<Theme>(() => {
    const q = new URLSearchParams(location.search).get('theme');
    return q === 'light' || q === 'dark' ? q : 'dark';
  });
  const resolved = useResolvedTheme(theme);
  const [nav, setNav] = useState('home');
  const [source, setSource] = useState(0);
  const [query, setQuery] = useState('');
  const [playing, setPlaying] = useState(true);
  const [pos, setPos] = useState(134);
  const [vol, setVol] = useState(72);
  const [liked, setLiked] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [exclusive, setExclusive] = useState(true);
  const [autoBest, setAutoBest] = useState(false);
  const [gapless, setGapless] = useState(true);
  const [tray, setTray] = useState(true);

  // Fake playback clock so the seek bar moves in the mockup.
  useEffect(() => {
    if (!playing) return;
    const id = setInterval(() => setPos((p) => (p + 1) % nowPlaying.duration), 1000);
    return () => clearInterval(id);
  }, [playing]);

  const [bStart, bEnd] = nowPlaying.bestPart;
  const inBest = pos >= bStart && pos < bEnd;
  const themeIdx = useMemo(() => (['system', 'light', 'dark'] as Theme[]).indexOf(theme), [theme]);

  return (
    <div className="app" data-resolved={resolved}>
      <div className="ambient" aria-hidden="true" style={{ backgroundImage: cover(nowPlaying.art) }} />
      <div className="scrim" aria-hidden="true" />

      {/* Custom title bar. In Tauri this becomes the drag region (decorations: false). */}
      <header className="titlebar" data-tauri-drag-region>
        <div className="brand">
          <span className="brand-mark" style={{ background: ACCENT }}><I.WaveIcon /></span>
          <span>ClaudeWaves</span>
        </div>
        <div className="history">
          <GlassButton variant="clear" size="sm" icon={<I.BackIcon />} aria-label="Back" />
          <GlassButton variant="clear" size="sm" icon={<I.ForwardIcon />} aria-label="Forward" disabled />
        </div>
        <div className="search">
          <GlassInput
            type="search"
            placeholder="Search songs, albums, artists"
            aria-label="Search songs, albums, artists"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onClear={() => setQuery('')}
            tint={ACCENT}
            trailing={<kbd className="kbd">Ctrl K</kbd>}
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
          <span className="avatar" aria-hidden="true">A</span>
        </div>
        <div className="win-controls">
          <button className="win-btn" aria-label="Minimize"><I.WinMin /></button>
          <button className="win-btn" aria-label="Maximize"><I.WinMax /></button>
          <button className="win-btn win-close" aria-label="Close to tray"><I.WinClose /></button>
        </div>
      </header>

      <div className="body">
        <GlassCard className="sidebar" padding="12px 10px" radius="24px">
          <nav aria-label="Main" className="nav">
            {NAV.map((n) =>
              nav === n.key ? (
                <GlassButton key={n.key} icon={n.icon} className="nav-item" aria-current="page">
                  {n.label}
                </GlassButton>
              ) : (
                <button key={n.key} className="row nav-plain" onClick={() => setNav(n.key)}>
                  {n.icon}
                  <span>{n.label}</span>
                </button>
              ),
            )}
          </nav>

          <div className="divider" />
          <div className="section-head">
            <span className="eyebrow">Playlists</span>
            <GlassButton variant="clear" size="sm" icon={<I.PlusIcon />} aria-label="New playlist" />
          </div>
          <ul className="playlists">
            {playlists.map((p) => (
              <li key={p.name}>
                <button className="row">
                  <span className="art" style={{ backgroundImage: cover(p.art), width: 36, height: 36, borderRadius: 9 }} />
                  <span className="row-text">
                    <span className="row-title">{p.name}</span>
                    <span className="row-sub">{p.meta}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>

          <div className="spacer" />
          <GlassCard className="device" padding="12px" radius="16px" variant="clear">
            <span className="device-icon" style={{ color: ACCENT }}><I.DacIcon /></span>
            <span className="row-text">
              <span className="row-title">USB DAC</span>
              <span className="row-sub">{exclusive ? 'Exclusive · bit-perfect' : 'Shared · 48 kHz mix'}</span>
            </span>
            <span className="dot" title="Connected" />
          </GlassCard>
        </GlassCard>

        <main className="content">
          <div className="content-head">
            <div>
              <div className="date">Saturday, 21 November</div>
              <h1>Good evening</h1>
            </div>
            <GlassSegmented
              aria-label="Source"
              options={['All', 'YouTube Music', 'Local']}
              value={source}
              onChange={setSource}
            />
          </div>

          <section aria-labelledby="qp">
            <h2 id="qp">Quick picks</h2>
            <div className="quick-grid">
              {quickPicks.map((t) => (
                <button key={t.title} className="quick">
                  <span className="art" style={{ backgroundImage: cover(t.art), width: 52, height: 52, borderRadius: 11 }} />
                  <span className="row-text">
                    <span className="row-title">{t.title}</span>
                    <span className="row-sub">{t.artist}</span>
                  </span>
                  <span className="row-sub">{fmt(t.duration)}</span>
                </button>
              ))}
            </div>
          </section>

          <section aria-labelledby="mx">
            <div className="section-row">
              <h2 id="mx">Mixed for you</h2>
              <GlassButton variant="clear" size="sm">Show all</GlassButton>
            </div>
            <div className="mix-grid">
              {mixes.map((m) => (
                <button key={m.title} className="mix">
                  <span className="mix-art" style={{ backgroundImage: cover(m.art) }} />
                  <span className="row-title">{m.title}</span>
                  <span className="row-sub">{m.sub}</span>
                </button>
              ))}
            </div>
          </section>

          <section aria-labelledby="ar">
            <h2 id="ar">Listen again</h2>
            <div className="mix-grid">
              {[...mixes].reverse().map((m) => (
                <button key={m.title} className="mix">
                  <span className="mix-art round" style={{ backgroundImage: cover(m.art + 3) }} />
                  <span className="row-title center">{m.title}</span>
                </button>
              ))}
            </div>
          </section>
          <div className="player-clearance" />
        </main>

        <GlassCard className="aside" padding="14px" radius="24px">
          <div className="now-art" style={{ backgroundImage: cover(nowPlaying.art) }}>
            <div className="badges">
              <span className="badge">{nowPlaying.format}</span>
              <span className="badge">Hi-Res</span>
            </div>
          </div>
          <div className="now-meta">
            <div className="now-title">{nowPlaying.title}</div>
            <div className="row-sub">{nowPlaying.artist} · {nowPlaying.album}</div>
          </div>
          <GlassButton
            variant={inBest ? 'tinted-filled' : 'tinted'}
            tint={ACCENT}
            icon={<I.SparkleIcon />}
            onClick={() => setPos(bStart)}
          >
            {inBest ? `In the Best Part · until ${fmt(bEnd)}` : `Jump to Best Part · ${fmt(bStart)}`}
          </GlassButton>
          <div className="section-head">
            <span className="eyebrow">Up next</span>
            <span className="row-sub">Autoplay on</span>
          </div>
          <ol className="queue">
            {upNext.map((t) => (
              <li key={t.title}>
                <button className="row">
                  <span className="art" style={{ backgroundImage: cover(t.art), width: 40, height: 40, borderRadius: 9 }} />
                  <span className="row-text">
                    <span className="row-title">{t.title}</span>
                    <span className="row-sub">{t.artist}</span>
                  </span>
                  <span className="row-sub">{fmt(t.duration)}</span>
                </button>
              </li>
            ))}
          </ol>
        </GlassCard>

        {/* Floating player over the scrolling content, so the glass has something to bend. */}
        <GlassCard className="player" padding="10px 16px 10px 10px" radius="28px">
          <div className="player-left">
            <span className="art" style={{ backgroundImage: cover(nowPlaying.art), width: 54, height: 54, borderRadius: 12 }} />
            <span className="row-text">
              <span className="row-title">{nowPlaying.title}</span>
              <span className="row-sub">{nowPlaying.artist}</span>
            </span>
            <GlassButton
              variant="clear"
              size="sm"
              icon={<I.HeartIcon />}
              aria-label={liked ? 'Unlike' : 'Like'}
              aria-pressed={liked}
              style={{ color: liked ? ACCENT : undefined }}
              onClick={() => setLiked(!liked)}
            />
          </div>

          <div className="player-center">
            <div className="transport">
              <GlassButton variant="clear" size="sm" icon={<I.ShuffleIcon />} aria-label="Shuffle" />
              <GlassButton variant="clear" icon={<I.PrevIcon />} aria-label="Previous" onClick={() => setPos(0)} />
              <GlassButton
                size="lg"
                icon={playing ? <I.PauseIcon /> : <I.PlayIcon />}
                aria-label={playing ? 'Pause' : 'Play'}
                onClick={() => setPlaying(!playing)}
              />
              <GlassButton variant="clear" icon={<I.NextIcon />} aria-label="Next" />
              <GlassButton variant="clear" size="sm" icon={<I.RepeatIcon />} aria-label="Repeat" />
            </div>
            <div className="seek">
              <span className="time right">{fmt(pos)}</span>
              <div className="seek-track">
                <span
                  className="best-marker"
                  aria-hidden="true"
                  style={{
                    left: `${(bStart / nowPlaying.duration) * 100}%`,
                    width: `${((bEnd - bStart) / nowPlaying.duration) * 100}%`,
                    background: ACCENT,
                  }}
                />
                <GlassSlider
                  aria-label="Seek"
                  min={0}
                  max={nowPlaying.duration}
                  value={pos}
                  onChange={setPos}
                  tint={resolved === 'dark' ? '#ffffff' : '#1c1c1e'}
                />
              </div>
              <span className="time">{fmt(nowPlaying.duration)}</span>
            </div>
          </div>

          <div className="player-right">
            <span className={`chip${exclusive ? '' : ' off'}`} style={{ color: ACCENT, borderColor: ACCENT }}>
              {exclusive ? 'BIT-PERFECT' : 'SHARED'}
            </span>
            <GlassButton variant="clear" size="sm" icon={<I.LyricsIcon />} aria-label="Lyrics" />
            <GlassButton variant="clear" size="sm" icon={<I.QueueIcon />} aria-label="Queue" />
            <GlassMenu
              aria-label="More"
              size="sm"
              icon={<I.MoreIcon />}
              align="end"
              items={[
                { label: 'Add to playlist', icon: <I.PlusIcon /> },
                { label: 'Start radio', icon: <I.WaveIcon /> },
                { label: 'Go to artist', icon: <I.ExploreIcon /> },
                { label: 'Exclusive output', icon: <I.DacIcon />, checked: exclusive, onSelect: () => setExclusive(!exclusive) },
              ]}
            />
            <span className="vol-icon"><I.VolumeIcon /></span>
            <div className="vol">
              <GlassSlider aria-label="Volume" min={0} max={100} value={vol} onChange={setVol} tint={ACCENT} />
            </div>
          </div>
        </GlassCard>
      </div>

      <GlassSheet open={settingsOpen} onClose={() => setSettingsOpen(false)} aria-label="Settings">
        <div className="sheet">
          <div className="sheet-head">
            <h2>Settings</h2>
            <GlassButton size="sm" onClick={() => setSettingsOpen(false)}>Done</GlassButton>
          </div>
          <div className="setting">
            <span className="row-text">
              <span className="row-title">Appearance</span>
              <span className="row-sub">Follow Windows, or pick one</span>
            </span>
            <GlassSegmented
              aria-label="Appearance"
              size="sm"
              options={['System', 'Light', 'Dark']}
              value={themeIdx}
              onChange={(i) => setTheme((['system', 'light', 'dark'] as Theme[])[i])}
            />
          </div>
          <Setting title="Exclusive output (WASAPI)" sub="Bit-perfect to your DAC. Other apps go quiet while playing." checked={exclusive} onChange={setExclusive} />
          <Setting title="Auto-jump to Best Part" sub="Start every song at its chorus" checked={autoBest} onChange={setAutoBest} />
          <Setting title="Gapless playback" sub="No silence between album tracks" checked={gapless} onChange={setGapless} />
          <Setting title="Keep playing in tray" sub="Closing the window hides it to the system tray" checked={tray} onChange={setTray} />
        </div>
      </GlassSheet>
    </div>
  );
}

function Setting(props: { title: string; sub: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="setting">
      <span className="row-text">
        <span className="row-title">{props.title}</span>
        <span className="row-sub">{props.sub}</span>
      </span>
      <GlassSwitch checked={props.checked} onChange={props.onChange} tint={ACCENT} aria-label={props.title} />
    </div>
  );
}
