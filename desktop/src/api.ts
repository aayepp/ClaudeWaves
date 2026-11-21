// Bridge to the Rust backend. Outside Tauri (plain `npm run dev` in a browser)
// it falls back to a demo library and a simulated clock so the UI still runs.

import { invoke } from '@tauri-apps/api/core';
import { listen, type UnlistenFn } from '@tauri-apps/api/event';
import { demoTracks } from './data';

export interface Track {
  path: string;
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
    default:
      return undefined as T;
  }
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
  if (t.bitDepth && t.sampleRate) return `${t.format} ${t.bitDepth}/${kHz(t.sampleRate)}`;
  if (t.bitrate) return `${t.format} ${t.bitrate} kbps`;
  return t.format;
}

export const isHiRes = (t: Track) => (t.bitDepth ?? 0) >= 24 || (t.sampleRate ?? 0) > 48000;

export const albumKey = (t: Track) => `${(t.albumArtist || t.artist).toLowerCase()}\u0000${t.album.toLowerCase()}`;
