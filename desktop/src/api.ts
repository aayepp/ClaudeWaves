// Bridge to the Rust backend. Outside Tauri (plain `npm run dev` in a browser)
// it falls back to a demo library and a simulated clock so the UI still runs.

import { invoke } from '@tauri-apps/api/core';
import { listen, type UnlistenFn } from '@tauri-apps/api/event';
import { demoTracks } from './data';

export interface Track {
  /** File path for local files, `yt:VIDEO_ID` for YouTube Music. */
  path: string;
  source?: 'local' | 'yt';
  videoId?: string;
  thumbnail?: string | null;
  artistId?: string | null;
  albumId?: string | null;
  title: string;
  artist: string;
  album: string;
  albumArtist: string;
  duration: number;
  trackNo: number | null;
  discNo: number | null;
  year: number | null;
  format: string;
  sampleRate: number | null;
  bitDepth: number | null;
  bitrate: number | null;
  hasCover: boolean;
  added: number;
}

export interface Library {
  folders: string[];
  tracks: Track[];
}

export interface PlayerState {
  path: string | null;
  playing: boolean;
  position: number;
  duration: number;
}

export interface OutputInfo {
  device: string;
  sampleRate: number;
  channels: number;
  sourceRate: number;
  resampling: boolean;
  mode: string;
}

export interface YtRef {
  name: string;
  id: string | null;
}

export interface YtItem {
  kind: 'song' | 'video' | 'album' | 'artist' | 'playlist' | 'podcast' | 'episode' | string;
  videoId: string | null;
  browseId: string | null;
  playlistId: string | null;
  title: string;
  subtitle: string;
  artists: YtRef[];
  album: YtRef | null;
  duration: number | null;
  thumbnail: string | null;
  explicit: boolean;
}

export interface YtShelf {
  title: string;
  more: string | null;
  items: YtItem[];
}

export interface YtHeader {
  title: string;
  subtitle: string;
  secondSubtitle: string;
  description: string;
  thumbnail: string | null;
  artists: YtRef[];
  playPlaylistId: string | null;
}

export interface YtPage {
  header: YtHeader | null;
  shelves: YtShelf[];
  chips: Array<{ title: string; browseId: string | null; params: string | null }>;
}

export interface Lyrics {
  synced: string | null;
  plain: string | null;
  instrumental: boolean;
}

export interface StreamInfo {
  key: string;
  codec: string;
  bitrate: number | null;
  sampleRate: number | null;
}

export interface YtDlpStatus {
  installed: boolean;
  version: string | null;
  path: string;
  jsRuntime: boolean;
}

export const isPlayable = (i: YtItem) => !!i.videoId && (i.kind === 'song' || i.kind === 'video' || i.kind === 'episode');

/** A YouTube Music item as a queue entry. */
export function ytTrack(i: YtItem, fallback?: { album?: string; albumId?: string | null; artist?: string; thumbnail?: string | null }): Track {
  const artist = i.artists.map((a) => a.name).join(', ') || fallback?.artist || i.subtitle || 'YouTube Music';
  return {
    path: `yt:${i.videoId}`,
    source: 'yt',
    videoId: i.videoId ?? undefined,
    thumbnail: i.thumbnail ?? fallback?.thumbnail ?? null,
    artistId: i.artists[0]?.id ?? null,
    albumId: i.album?.id ?? fallback?.albumId ?? null,
    title: i.title,
    artist,
    album: i.album?.name ?? fallback?.album ?? '',
    albumArtist: '',
    duration: i.duration ?? 0,
    trackNo: null,
    discNo: null,
    year: null,
    format: i.kind === 'video' ? 'Video' : 'YouTube Music',
    sampleRate: null,
    bitDepth: null,
    bitrate: null,
    hasCover: false,
    added: 0,
  };
}

export const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;

// ---------------------------------------------------------------------------
// Demo backend (browser only)
// ---------------------------------------------------------------------------

type Handler<T> = (payload: T) => void;
const demoListeners = new Map<string, Set<Handler<unknown>>>();
const demo = {
  state: { path: null, playing: false, position: 0, duration: 0 } as PlayerState,
  timer: 0 as unknown as ReturnType<typeof setInterval>,
  emit(event: string, payload: unknown) {
    demoListeners.get(event)?.forEach((h) => h(payload));
  },
  tick() {
    clearInterval(demo.timer);
    if (!demo.state.playing) return;
    demo.timer = setInterval(() => {
      demo.state.position += 0.25;
      if (demo.state.position >= demo.state.duration) {
        demo.state = { ...demo.state, playing: false, position: demo.state.duration };
        clearInterval(demo.timer);
        demo.emit('player://state', { ...demo.state });
        demo.emit('player://ended', demo.state.path);
        return;
      }
      demo.emit('player://state', { ...demo.state });
    }, 250);
  },
};

const demoOutput: OutputInfo = {
  device: 'Speakers (Demo)',
  sampleRate: 48000,
  channels: 2,
  sourceRate: 44100,
  resampling: true,
  mode: 'Browser preview',
};

async function demoInvoke<T>(cmd: string, args: Record<string, unknown> = {}): Promise<T> {
  switch (cmd) {
    case 'library_get':
    case 'library_rescan':
      return { folders: ['C:\\Users\\You\\Music'], tracks: demoTracks } as T;
    case 'player_load': {
      const t = demoTracks.find((x) => x.path === args.path);
      demo.state = { path: String(args.path), playing: Boolean(args.autoplay), position: 0, duration: t?.duration ?? 200 };
      demo.emit('player://state', { ...demo.state });
      demo.tick();
      return undefined as T;
    }
    case 'player_play':
      demo.state.playing = !!demo.state.path;
      demo.emit('player://state', { ...demo.state });
      demo.tick();
      return undefined as T;
    case 'player_pause':
      demo.state.playing = false;
      demo.emit('player://state', { ...demo.state });
      demo.tick();
      return undefined as T;
    case 'player_seek':
      demo.state.position = Number(args.seconds);
      demo.emit('player://state', { ...demo.state });
      return undefined as T;
    case 'output_info':
      return demoOutput as T;
    case 'output_devices':
      return ['Speakers (Demo)', 'USB DAC (Demo)'] as T;
    case 'track_cover':
      return null as T;
    case 'yt_home':
    case 'yt_search':
      return demoPage() as T;
    case 'yt_browse':
      return demoPage(true) as T;
    case 'yt_next':
      return demoItems().slice(1) as T;
    case 'yt_suggest':
      return ['lumen coast', 'lumen coast midnight tide', 'lumen coast live'] as T;
    case 'yt_play': {
      const t = demoTracks[0];
      demo.state = { path: `yt:${args.videoId}`, playing: true, position: 0, duration: Number(args.duration) || t.duration };
      demo.emit('player://state', { ...demo.state });
      demo.emit('player://stream', { key: demo.state.path, codec: 'AAC', bitrate: 256, sampleRate: 44100 });
      demo.tick();
      return undefined as T;
    }
    case 'lyrics_get':
      return { synced: null, plain: 'Lyrics show here in the desktop app.', instrumental: false } as T;
    case 'ytdlp_status':
      return { installed: false, version: null, path: '', jsRuntime: false } as T;
    default:
      return undefined as T;
  }
}

function demoItems(): YtItem[] {
  return demoTracks.map((t, i) => ({
    kind: 'song',
    videoId: `demo${i}`,
    browseId: null,
    playlistId: null,
    title: t.title,
    subtitle: `${t.artist} • ${t.album}`,
    artists: [{ name: t.artist, id: 'demo-artist' }],
    album: { name: t.album, id: 'demo-album' },
    duration: t.duration,
    thumbnail: null,
    explicit: false,
  }));
}

function demoPage(withHeader = false): YtPage {
  const songs = demoItems();
  const albums: YtItem[] = [...new Set(demoTracks.map((t) => t.album))].map((a) => ({
    ...songs.find((s) => s.album?.name === a)!,
    kind: 'album',
    videoId: null,
    browseId: 'demo-album',
    title: a,
    subtitle: `Album • ${demoTracks.find((t) => t.album === a)!.artist}`,
  }));
  return {
    header: withHeader
      ? { title: 'Harbor Lights', subtitle: 'Album • 2024', secondSubtitle: '10 songs • 40 minutes', description: '', thumbnail: null, artists: [{ name: 'Lumen Coast', id: null }], playPlaylistId: null }
      : null,
    shelves: withHeader
      ? [{ title: '', more: null, items: songs }]
      : [
          { title: 'Quick picks', more: null, items: songs },
          { title: 'Albums for you', more: null, items: albums },
          { title: 'Trending', more: null, items: [...songs].reverse().map((s) => ({ ...s, kind: 'video' })) },
        ],
    chips: ['Energize', 'Relax', 'Workout', 'Feel good', 'Focus', 'Sleep'].map((title) => ({ title, browseId: 'FEmusic_home', params: title })),
  };
}

function call<T>(cmd: string, args?: Record<string, unknown>): Promise<T> {
  return isTauri ? invoke<T>(cmd, args) : demoInvoke<T>(cmd, args);
}

export function on<T>(event: string, handler: Handler<T>): () => void {
  if (!isTauri) {
    const set = demoListeners.get(event) ?? new Set();
    set.add(handler as Handler<unknown>);
    demoListeners.set(event, set);
    return () => set.delete(handler as Handler<unknown>);
  }
  let un: UnlistenFn | null = null;
  let dead = false;
  listen<T>(event, (e) => handler(e.payload)).then((u) => {
    if (dead) u();
    else un = u;
  });
  return () => {
    dead = true;
    un?.();
  };
}

// ---------------------------------------------------------------------------
// Commands
// ---------------------------------------------------------------------------

export const api = {
  library: () => call<Library>('library_get'),
  addFolder: (folder: string) => call<Library>('library_add_folder', { folder }),
  removeFolder: (folder: string) => call<Library>('library_remove_folder', { folder }),
  rescan: () => call<Library>('library_rescan'),
  cover: (path: string) => call<string | null>('track_cover', { path }),
  readTracks: (paths: string[]) => call<Track[]>('read_tracks', { paths }),

  load: (path: string, autoplay = true) => call<void>('player_load', { path, autoplay }),
  play: () => call<void>('player_play'),
  pause: () => call<void>('player_pause'),
  seek: (seconds: number) => call<void>('player_seek', { seconds }),
  volume: (volume: number) => call<void>('player_volume', { volume }),
  stop: () => call<void>('player_stop'),
  outputInfo: () => call<OutputInfo>('output_info'),
  outputDevices: () => call<string[]>('output_devices'),
  setOutputDevice: (name: string | null) => call<void>('set_output_device', { name }),

  ytHome: (params?: string | null) => call<YtPage>('yt_home', { params: params ?? null }),
  ytSearch: (query: string, filter?: string | null) => call<YtPage>('yt_search', { query, filter: filter ?? null }),
  ytSuggest: (query: string) => call<string[]>('yt_suggest', { query }),
  ytBrowse: (browseId: string, params?: string | null) => call<YtPage>('yt_browse', { browseId, params: params ?? null }),
  ytNext: (videoId: string, playlistId?: string | null) => call<YtItem[]>('yt_next', { videoId, playlistId: playlistId ?? null }),
  ytPlay: (videoId: string, duration: number | null, autoplay = true) => call<void>('yt_play', { videoId, duration, autoplay }),
  ytRegion: (hl: string, gl: string) => call<void>('yt_region', { hl, gl }),
  ytdlpStatus: () => call<YtDlpStatus>('ytdlp_status'),
  ytdlpInstall: () => call<YtDlpStatus>('ytdlp_install'),
  lyrics: (t: Track) => call<Lyrics>('lyrics_get', { title: t.title, artist: t.artist, album: t.album, duration: t.duration }),

  nowPlaying: (t: Track) =>
    call<void>('media_now_playing', { track: { title: t.title, artist: t.artist, album: t.album, duration: t.duration } }),
  mediaPlayback: (playing: boolean, position: number) => call<void>('media_playback', { playing, position }),
  setCloseToTray: (enabled: boolean) => call<void>('set_close_to_tray', { enabled }),
};

export async function pickFolder(): Promise<string | null> {
  if (!isTauri) return null;
  const { open } = await import('@tauri-apps/plugin-dialog');
  const res = await open({ directory: true, multiple: false, title: 'Choose a music folder' });
  return typeof res === 'string' ? res : null;
}

export async function windowAction(action: 'minimize' | 'maximize' | 'close') {
  if (!isTauri) return;
  const { getCurrentWindow } = await import('@tauri-apps/api/window');
  const w = getCurrentWindow();
  if (action === 'minimize') await w.minimize();
  else if (action === 'maximize') await w.toggleMaximize();
  else await w.close();
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

export const fmt = (t: number) => {
  if (!Number.isFinite(t) || t < 0) t = 0;
  const h = Math.floor(t / 3600);
  const m = Math.floor((t % 3600) / 60);
  const s = String(Math.floor(t % 60)).padStart(2, '0');
  return h ? `${h}:${String(m).padStart(2, '0')}:${s}` : `${m}:${s}`;
};

export const kHz = (hz: number) => `${+(hz / 1000).toFixed(1)}`;

export function qualityLabel(t: Track): string {
  if (t.source === 'yt') return t.format;
  if (t.bitDepth && t.sampleRate) return `${t.format} ${t.bitDepth}/${kHz(t.sampleRate)}`;
  if (t.bitrate) return `${t.format} ${t.bitrate} kbps`;
  return t.format;
}

export const isHiRes = (t: Track) => (t.bitDepth ?? 0) >= 24 || (t.sampleRate ?? 0) > 48000;

export const albumKey = (t: Track) => `${(t.albumArtist || t.artist).toLowerCase()}\u0000${t.album.toLowerCase()}`;
