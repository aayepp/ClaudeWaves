// Demo library for running the UI in a plain browser (`npm run dev`).
// Inside the desktop app the real library comes from the Rust backend.

import type { Track } from './api';

const palettes = [
  ['#3b1d8f', '#0c5f73', '#ff8a5c', 140],
  ['#1d3b8f', '#7ad3ff', '#ffffff', 160],
  ['#0f5132', '#a3e635', '#fef08a', 120],
  ['#7c2d12', '#f97316', '#fde68a', 200],
  ['#111827', '#6366f1', '#f0abfc', 110],
  ['#134e4a', '#2dd4bf', '#e0f2fe', 170],
  ['#831843', '#f472b6', '#fde047', 135],
  ['#1e1b4b', '#0ea5e9', '#a5b4fc', 95],
] as const;

/** Placeholder art for tracks without a cover, picked from a hash of the key. */
export function placeholderArt(key: string): string {
  let h = 0;
  for (let i = 0; i < key.length; i++) h = (h * 31 + key.charCodeAt(i)) | 0;
  const [c1, c2, c3, ang] = palettes[Math.abs(h) % palettes.length];
  return `radial-gradient(circle at 28% 30%, ${c3} 0, transparent 46%), radial-gradient(circle at 78% 80%, ${c1} 0, transparent 52%), linear-gradient(${ang}deg, ${c1}, ${c2})`;
}

const rows: Array<[string, string, string, number, string, number | null, number | null, number]> = [
  ['Midnight Tide', 'Lumen Coast', 'Harbor Lights', 232, 'FLAC', 24, 96000, 1],
  ['Harbor Lights', 'Lumen Coast', 'Harbor Lights', 312, 'FLAC', 24, 96000, 2],
  ['Paper Satellites', 'Nora Vale', 'Small Hours', 247, 'FLAC', 16, 44100, 1],
  ['Glasshouse', 'The Quiet Hours', 'Glasshouse', 211, 'MP3', null, 44100, 1],
  ['Afterglow Drive', 'Kite & Ember', 'Neon Fields', 285, 'FLAC', 16, 44100, 3],
  ['Neon Fields', 'Kite & Ember', 'Neon Fields', 241, 'FLAC', 16, 44100, 1],
  ['Low Orbit', 'Sable Youth', 'Low Orbit', 178, 'M4A', null, 44100, 1],
  ['Rain on Tin', 'Hana Mori', 'Weather Studies', 199, 'FLAC', 24, 48000, 4],
  ['Still Water', 'Hana Mori', 'Weather Studies', 263, 'FLAC', 24, 48000, 5],
  ['Signal Fire', 'Nora Vale', 'Small Hours', 224, 'FLAC', 16, 44100, 2],
];

export const demoTracks: Track[] = rows.map(([title, artist, album, duration, format, bitDepth, sampleRate, trackNo], i) => ({
  path: `demo://${album}/${title}`,
  title,
  artist,
  album,
  albumArtist: artist,
  duration,
  trackNo,
  discNo: 1,
  year: 2024,
  format,
  sampleRate,
  bitDepth,
  bitrate: bitDepth ? null : 320,
  hasCover: false,
  added: 1_700_000_000 + i * 1000,
}));
