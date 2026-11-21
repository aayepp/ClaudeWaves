import { useEffect, useRef, useState, type CSSProperties } from 'react';
import { api, type Track } from './api';
import { placeholderArt } from './data';

// One request per file, shared by every component that shows it.
const cache = new Map<string, Promise<string | null>>();

export function coverFor(t: Track | null | undefined): Promise<string | null> {
  if (t?.thumbnail) return Promise.resolve(t.thumbnail);
  if (!t || !t.hasCover) return Promise.resolve(null);
  let p = cache.get(t.path);
  if (!p) {
    p = api.cover(t.path).catch(() => null);
    cache.set(t.path, p);
  }
  return p;
}

export function useCover(t: Track | null | undefined, enabled = true): string | null {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    setUrl(null);
    if (enabled) coverFor(t).then((u) => alive && setUrl(u));
    return () => {
      alive = false;
    };
  }, [t?.path, t?.hasCover, t?.thumbnail, enabled]);
  return url;
}

/** True once the element has scrolled near the viewport; covers load lazily. */
function useSeen<T extends Element>(): [React.RefObject<T>, boolean] {
  const ref = useRef<T>(null);
  const [seen, setSeen] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el || seen) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          setSeen(true);
          io.disconnect();
        }
      },
      { rootMargin: '300px' },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [seen]);
  return [ref, seen];
}

/** Background value: the real cover when there is one, else placeholder art. */
export function artBackground(t: Track | null | undefined, url: string | null): string {
  if (url) return `center / cover no-repeat url("${url}")`;
  return placeholderArt(t ? `${t.albumArtist || t.artist}/${t.album}` : 'empty');
}

export function Art({
  track,
  size,
  radius = 10,
  className = 'art',
  style,
  children,
}: {
  track: Track | null | undefined;
  size?: number;
  radius?: number | string;
  className?: string;
  style?: CSSProperties;
  children?: React.ReactNode;
}) {
  const [ref, seen] = useSeen<HTMLSpanElement>();
  const url = useCover(track, seen);
  return (
    <span
      ref={ref}
      className={className}
      style={{
        background: artBackground(track, url),
        width: size,
        height: size,
        borderRadius: radius,
        ...style,
      }}
    >
      {children}
    </span>
  );
}
