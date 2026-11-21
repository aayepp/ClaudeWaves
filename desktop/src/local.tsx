// Local library screens: songs, albums, album page, folders.

import { useMemo, useState } from 'react';
import { GlassButton, GlassCard, GlassSegmented } from '@zakisheriff/liquid-glass';
import * as I from './icons';
import { fmt, isHiRes, isTauri, qualityLabel, type Library, type Track } from './api';
import { Art } from './covers';
import type { Player } from './player';

const ACCENT = '#7C4DFF';

export interface Album {
  key: string;
  title: string;
  artist: string;
  year: number | null;
  tracks: Track[];
}

// ---------------------------------------------------------------------------
// Views
// ---------------------------------------------------------------------------

export function TrackRow({ t, n, onPlay, active, showAlbum = true }: { t: Track; n?: number; onPlay: () => void; active: boolean; showAlbum?: boolean }) {
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

export function LocalHome({ library, albums, player, onAlbum }: { library: Library; albums: Album[]; player: Player; onAlbum: (k: string) => void }) {
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
          <h1>Your library</h1>
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

export function SongsView({ tracks, player }: { tracks: Track[]; player: Player }) {
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

export function AlbumGrid({ albums, onAlbum }: { albums: Album[]; onAlbum: (k: string) => void }) {
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

export function AlbumsView({ albums, onAlbum }: { albums: Album[]; onAlbum: (k: string) => void }) {
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

export function AlbumView({ album, player, onBack }: { album: Album; player: Player; onBack: () => void }) {
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

export function LocalSearch({ query, results, player }: { query: string; results: Track[]; player: Player }) {
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

export function FoldersView(props: {
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

export function EmptyLibrary({ onAdd, scanning }: { onAdd: () => void; scanning: number | null }) {
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

