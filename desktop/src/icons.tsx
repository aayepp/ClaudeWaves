import type { ReactNode } from 'react';

const S = ({ children, size = 18, fill = false }: { children: ReactNode; size?: number; fill?: boolean }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill={fill ? 'currentColor' : 'none'}
    stroke={fill ? 'none' : 'currentColor'}
    strokeWidth={2}
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    {children}
  </svg>
);

export const HomeIcon = () => <S><path d="M3 10.5L12 3l9 7.5V20a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1z" /></S>;
export const ExploreIcon = () => <S><circle cx="12" cy="12" r="9" /><path d="M15.5 8.5l-2 5-5 2 2-5z" /></S>;
export const LibraryIcon = () => <S><path d="M4 4v16M9 4v16M14 4l6 16" /></S>;
export const FolderIcon = () => <S><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" /></S>;
export const DownloadIcon = () => <S><path d="M12 4v11M7 10l5 5 5-5M5 20h14" /></S>;
export const PlusIcon = () => <S size={16}><path d="M12 5v14M5 12h14" /></S>;
export const SunIcon = () => <S size={17}><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></S>;
export const MoonIcon = () => <S size={17}><path d="M20 14.5A8 8 0 019.5 4 8 8 0 1020 14.5z" /></S>;
export const BackIcon = () => <S size={16}><path d="M15 18l-6-6 6-6" /></S>;
export const ForwardIcon = () => <S size={16}><path d="M9 18l6-6-6-6" /></S>;
export const PlayIcon = () => <S size={22} fill><path d="M8 5v14l11-7z" /></S>;
export const PauseIcon = () => <S size={22} fill><rect x="6" y="5" width="4" height="14" rx="1" /><rect x="14" y="5" width="4" height="14" rx="1" /></S>;
export const PrevIcon = () => <S size={20} fill><path d="M6 5h2v14H6zM20 5v14L9 12z" /></S>;
export const NextIcon = () => <S size={20} fill><path d="M16 5h2v14h-2zM4 5v14l11-7z" /></S>;
export const ShuffleIcon = () => <S size={17}><path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5" /></S>;
export const RepeatIcon = () => <S size={17}><path d="M17 2l4 4-4 4M3 11V9a3 3 0 013-3h15M7 22l-4-4 4-4M21 13v2a3 3 0 01-3 3H3" /></S>;
export const HeartIcon = () => <S size={18} fill><path d="M12 21s-7.5-4.6-9.5-9.3C1.2 8.4 3.3 5 6.8 5c2 0 3.4 1.1 4.2 2.4h2C13.8 6.1 15.2 5 17.2 5c3.5 0 5.6 3.4 4.3 6.7C19.5 16.4 12 21 12 21z" /></S>;
export const LyricsIcon = () => <S size={17}><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /><path d="M8 9h8M8 13h5" /></S>;
export const QueueIcon = () => <S size={17}><path d="M3 6h13M3 12h13M3 18h9M19 15v6M16 18h6" /></S>;
export const DacIcon = () => <S size={17}><rect x="5" y="2" width="14" height="20" rx="2" /><circle cx="12" cy="14" r="3" /><path d="M12 6h.01" /></S>;
export const VolumeIcon = () => <S size={17}><path d="M11 5L6 9H2v6h4l5 4z" /><path d="M15.5 8.5a5 5 0 010 7M19 5a10 10 0 010 14" /></S>;
export const SparkleIcon = () => <S size={16}><path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z" /><path d="M19 17l.7 1.8 1.8.7-1.8.7L19 22l-.7-1.8-1.8-.7 1.8-.7z" /></S>;
export const SettingsIcon = () => <S size={17}><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.8-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1.1-1.5 1.7 1.7 0 00-1.8.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.8 1.7 1.7 0 00-1.5-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.5-1.1 1.7 1.7 0 00-.3-1.8l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.8.3H9a1.7 1.7 0 001-1.5V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.8-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.8V9a1.7 1.7 0 001.5 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.5 1z" /></S>;
export const MoreIcon = () => <S size={17} fill><circle cx="5" cy="12" r="1.8" /><circle cx="12" cy="12" r="1.8" /><circle cx="19" cy="12" r="1.8" /></S>;
export const WaveIcon = () => <S size={14}><path d="M3 12c2-4 4-4 6 0s4 4 6 0 4-4 6 0" /></S>;
export const CheckIcon = () => <S size={16}><path d="M5 12l5 5 9-10" /></S>;

export const WinMin = () => <svg width="10" height="10" viewBox="0 0 10 10" aria-hidden="true"><path d="M0 5h10" stroke="currentColor" /></svg>;
export const WinMax = () => <svg width="10" height="10" viewBox="0 0 10 10" aria-hidden="true"><rect x=".5" y=".5" width="9" height="9" rx="1.5" fill="none" stroke="currentColor" /></svg>;
export const WinClose = () => <svg width="10" height="10" viewBox="0 0 10 10" aria-hidden="true"><path d="M0 0l10 10M10 0L0 10" stroke="currentColor" /></svg>;
export const SongsIcon = () => <S><path d="M9 18V5l12-2v13" /><circle cx="6" cy="18" r="3" /><circle cx="18" cy="16" r="3" /></S>;
export const AlbumIcon = () => <S><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="2.5" /></S>;
export const YouTubeIcon = () => <S><rect x="2.5" y="5" width="19" height="14" rx="4" /><path d="M10 9.5v5l4.5-2.5z" /></S>;
export const TrashIcon = () => <S size={16}><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" /></S>;
export const RefreshIcon = () => <S size={16}><path d="M21 12a9 9 0 11-3-6.7L21 8M21 3v5h-5" /></S>;
export const RepeatOneIcon = () => <S size={17}><path d="M17 2l4 4-4 4M3 11V9a3 3 0 013-3h15M7 22l-4-4 4-4M21 13v2a3 3 0 01-3 3H3" /><path d="M11 10h1.5v5" /></S>;
export const AlertIcon = () => <S size={16}><circle cx="12" cy="12" r="9" /><path d="M12 7v6M12 16.5v.5" /></S>;
