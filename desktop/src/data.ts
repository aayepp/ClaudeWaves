// Sample content for the mockup. Real data comes from YouTube Music and local files later.

export type Palette = readonly [string, string, string, number];

const palettes: Palette[] = [
  ['#3b1d8f', '#0c5f73', '#ff8a5c', 140],
  ['#1d3b8f', '#7ad3ff', '#ffffff', 160],
  ['#0f5132', '#a3e635', '#fef08a', 120],
  ['#7c2d12', '#f97316', '#fde68a', 200],
  ['#111827', '#6366f1', '#f0abfc', 110],
  ['#134e4a', '#2dd4bf', '#e0f2fe', 170],
  ['#831843', '#f472b6', '#fde047', 135],
  ['#1e1b4b', '#0ea5e9', '#a5b4fc', 95],
];

/** Placeholder cover art built from gradients, standing in for real thumbnails. */
export function cover(i: number): string {
  const [c1, c2, c3, ang] = palettes[i % palettes.length];
  return `radial-gradient(circle at 28% 30%, ${c3} 0, transparent 46%), radial-gradient(circle at 78% 80%, ${c1} 0, transparent 52%), linear-gradient(${ang}deg, ${c1}, ${c2})`;
}

export interface Track {
  title: string;
  artist: string;
  duration: number; // seconds
  art: number;
}

export const nowPlaying: Track & { album: string; format: string; bestPart: [number, number] } = {
  title: 'Midnight Tide',
  artist: 'Lumen Coast',
  album: 'Harbor Lights',
  duration: 232,
  art: 0,
  format: 'FLAC 24/96',
  bestPart: [88, 128],
};

export const quickPicks: Track[] = [
  { title: 'Midnight Tide', artist: 'Lumen Coast', duration: 232, art: 0 },
  { title: 'Paper Satellites', artist: 'Nora Vale', duration: 247, art: 1 },
  { title: 'Glasshouse', artist: 'The Quiet Hours', duration: 211, art: 2 },
  { title: 'Afterglow Drive', artist: 'Kite & Ember', duration: 285, art: 3 },
  { title: 'Low Orbit', artist: 'Sable Youth', duration: 178, art: 4 },
  { title: 'Rain on Tin', artist: 'Hana Mori', duration: 199, art: 5 },
];

export const mixes = [
  { title: 'Focus Flow', sub: 'Instrumental · 50 songs', art: 3 },
  { title: 'Late Night', sub: 'Lo-fi & chillhop', art: 4 },
  { title: 'Hi-Res Essentials', sub: '24-bit lossless picks', art: 5 },
  { title: 'Best Parts Only', sub: 'Jumps to every chorus', art: 6 },
  { title: 'Daily Mix 1', sub: 'Lumen Coast, Nora Vale', art: 7 },
  { title: 'Rainy Day', sub: 'Soft acoustic', art: 2 },
  { title: 'Night Drive', sub: 'Synthwave & retro', art: 1 },
  { title: 'Deep Work', sub: 'Ambient · no vocals', art: 0 },
];

export const upNext: Track[] = [
  { title: 'Paper Satellites', artist: 'Nora Vale', duration: 247, art: 1 },
  { title: 'Glasshouse', artist: 'The Quiet Hours', duration: 211, art: 2 },
  { title: 'Low Orbit', artist: 'Sable Youth', duration: 178, art: 4 },
  { title: 'Harbor Lights', artist: 'Lumen Coast', duration: 312, art: 0 },
  { title: 'Rain on Tin', artist: 'Hana Mori', duration: 199, art: 5 },
  { title: 'Neon Fields', artist: 'Kite & Ember', duration: 241, art: 6 },
];

export const playlists = [
  { name: 'Liked songs', meta: '412 songs', art: 6 },
  { name: 'Work focus', meta: '88 songs', art: 5 },
  { name: 'Drive home', meta: '41 songs', art: 3 },
  { name: 'Hi-Res 24-bit', meta: 'Local · 126 files', art: 7 },
];

export const fmt = (t: number) => `${Math.floor(t / 60)}:${String(Math.floor(t % 60)).padStart(2, '0')}`;
