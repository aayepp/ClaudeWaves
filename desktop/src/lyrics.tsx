import { useEffect, useMemo, useRef, useState } from 'react';
import { api, type Lyrics, type Track } from './api';
import type { Player } from './player';

interface Line {
  time: number;
  text: string;
}

/** Parses LRC text: "[01:23.45] words". Lines may carry several stamps. */
export function parseLrc(lrc: string): Line[] {
  const out: Line[] = [];
  for (const raw of lrc.split(/\r?\n/)) {
    const stamps = [...raw.matchAll(/\[(\d+):(\d+(?:\.\d+)?)\]/g)];
    if (!stamps.length) continue;
    const text = raw.replace(/\[[^\]]*\]/g, '').trim();
    for (const m of stamps) out.push({ time: Number(m[1]) * 60 + Number(m[2]), text });
  }
  return out.sort((a, b) => a.time - b.time);
}

const cache = new Map<string, Promise<Lyrics>>();
function lyricsFor(t: Track): Promise<Lyrics> {
  let p = cache.get(t.path);
  if (!p) {
    p = api.lyrics(t).catch(() => ({ synced: null, plain: null, instrumental: false }));
    cache.set(t.path, p);
  }
  return p;
}

export function LyricsView({ player }: { player: Player }) {
  const t = player.current;
  const [lyrics, setLyrics] = useState<Lyrics | null>(null);
  const [loading, setLoading] = useState(false);
  const box = useRef<HTMLDivElement>(null);
  const userScrolled = useRef(0);

  useEffect(() => {
    let alive = true;
    setLyrics(null);
    if (!t) return;
    setLoading(true);
    lyricsFor(t).then((l) => {
      if (!alive) return;
      setLyrics(l);
      setLoading(false);
    });
    return () => {
      alive = false;
    };
  }, [t?.path]);

  const lines = useMemo(() => (lyrics?.synced ? parseLrc(lyrics.synced) : []), [lyrics]);
  const pos = player.state.position + 0.25; // small lead so the line lights as it is sung
  let active = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].time <= pos) active = i;
    else break;
  }

  // Keep the current line in view unless the user scrolled in the last few seconds.
  useEffect(() => {
    if (active < 0 || Date.now() - userScrolled.current < 4000) return;
    const el = box.current?.querySelector<HTMLElement>(`[data-line="${active}"]`);
    el?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }, [active]);

  if (!t) return <p className="row-sub pad">Play a song to see its lyrics.</p>;
  if (loading) return <p className="row-sub pad">Looking for lyrics…</p>;
  if (lyrics?.instrumental) return <p className="row-sub pad">Instrumental.</p>;
  if (!lines.length && lyrics?.plain) {
    return (
      <div className="lyrics plain" ref={box}>
        {lyrics.plain.split('\n').map((l, i) => (
          <p key={i}>{l || ' '}</p>
        ))}
      </div>
    );
  }
  if (!lines.length) return <p className="row-sub pad">No lyrics found for this song.</p>;

  return (
    <div
      className="lyrics"
      ref={box}
      onWheel={() => (userScrolled.current = Date.now())}
      onTouchMove={() => (userScrolled.current = Date.now())}
    >
      {lines.map((l, i) => (
        <button
          key={i}
          data-line={i}
          className={`lyric${i === active ? ' is-active' : i < active ? ' is-past' : ''}`}
          onClick={() => {
            userScrolled.current = 0;
            player.seek(l.time);
          }}
        >
          {l.text || '♪'}
        </button>
      ))}
      <p className="row-sub lyrics-credit">Lyrics from LRCLIB</p>
    </div>
  );
}
