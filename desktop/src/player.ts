// Queue and playback state. The Rust engine plays one source at a time; the
// queue, shuffle, repeat and YouTube radio live here and drive it.

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, on, ytTrack, type OutputInfo, type PlayerState, type StreamInfo, type Track } from './api';

export type Repeat = 'off' | 'all' | 'one';

function shuffled<T>(xs: T[], keepFirst?: T): T[] {
  const a = xs.filter((x) => x !== keepFirst);
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return keepFirst === undefined ? a : [keepFirst, ...a];
}

const readNum = (k: string, d: number) => {
  try {
    const raw = localStorage.getItem(k);
    const v = Number(raw);
    return raw !== null && Number.isFinite(v) ? v : d;
  } catch {
    return d;
  }
};

export function usePlayer(library: Map<string, Track>) {
  const [state, setState] = useState<PlayerState>({ path: null, playing: false, position: 0, duration: 0 });
  const [output, setOutput] = useState<OutputInfo | null>(null);
  const [stream, setStream] = useState<StreamInfo | null>(null);
  const [buffering, setBuffering] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<string[]>([]);
  const [base, setBase] = useState<string[]>([]);
  const [index, setIndex] = useState(-1);
  const [shuffle, setShuffle] = useState(false);
  const [repeat, setRepeat] = useState<Repeat>('off');
  const [volume, setVolumeState] = useState(() => readNum('cw.volume', 0.8));
  const [radioFor, setRadioFor] = useState<string | null>(null);

  // Every track the queue has seen, so YouTube entries resolve by key too.
  const known = useRef(new Map<string, Track>());
  const [knownVersion, setKnownVersion] = useState(0);
  const remember = useCallback((list: Track[]) => {
    list.forEach((t) => known.current.set(t.path, t));
    setKnownVersion((v) => v + 1);
  }, []);
  const lookup = useCallback((p: string | null | undefined) => (p ? known.current.get(p) ?? library.get(p) ?? null : null), [library]);

  const live = useRef({ order, index, repeat, state, radioFor });
  live.current = { order, index, repeat, state, radioFor };

  const current = useMemo(() => lookup(state.path ?? order[index]), [lookup, state.path, order, index, knownVersion]);

  const start = useCallback(
    (path: string, autoplay = true) => {
      const t = lookup(path);
      setError(null);
      if (t?.source === 'yt' && t.videoId) {
        setBuffering(path);
        api.ytPlay(t.videoId, t.duration || null, autoplay).catch((e) => setError(String(e)));
      } else {
        setStream(null);
        api.load(path, autoplay);
      }
    },
    [lookup],
  );

  const loadAt = useCallback(
    (list: string[], i: number, autoplay = true) => {
      if (i < 0 || i >= list.length) return;
      setIndex(i);
      start(list[i], autoplay);
    },
    [start],
  );

  /** Plays `list` from `startAt`; it becomes the queue. */
  const playList = useCallback(
    (list: Track[], startAt = 0) => {
      if (!list.length) return;
      remember(list);
      setRadioFor(null);
      const paths = list.map((t) => t.path);
      setBase(paths);
      const ord = shuffle ? shuffled(paths, paths[startAt]) : paths;
      setOrder(ord);
      loadAt(ord, shuffle ? 0 : startAt);
    },
    [shuffle, loadAt, remember],
  );

  /** Plays one YouTube track and fills the queue with its radio. */
  const playRadio = useCallback(
    async (t: Track) => {
      remember([t]);
      setBase([t.path]);
      setOrder([t.path]);
      setRadioFor(t.path);
      loadAt([t.path], 0);
      if (!t.videoId) return;
      try {
        const items = await api.ytNext(t.videoId);
        const extra = items.filter((i) => i.videoId && i.videoId !== t.videoId).map((i) => ytTrack(i));
        if (live.current.radioFor !== t.path) return;
        remember(extra);
        const paths = [t.path, ...extra.map((x) => x.path)];
        setBase(paths);
        setOrder((o) => (o[0] === t.path ? paths : o));
      } catch {
        /* radio is a bonus; the song itself still plays */
      }
    },
    [loadAt, remember],
  );

  /** Adds tracks right after the current one. */
  const playNext = useCallback(
    (list: Track[]) => {
      remember(list);
      const at = live.current.index + 1;
      const paths = list.map((t) => t.path);
      setOrder((o) => [...o.slice(0, at), ...paths, ...o.slice(at)]);
      setBase((b) => [...b, ...paths]);
    },
    [remember],
  );

  const addToQueue = useCallback(
    (list: Track[]) => {
      remember(list);
      const paths = list.map((t) => t.path);
      setOrder((o) => [...o, ...paths]);
      setBase((b) => [...b, ...paths]);
    },
    [remember],
  );

  const extendWithRadio = useCallback(async () => {
    const { order, index } = live.current;
    const last = lookup(order[index]);
    if (!last?.videoId) return false;
    try {
      const items = await api.ytNext(last.videoId);
      const seen = new Set(order);
      const extra = items.map((i) => ytTrack(i)).filter((x) => x.videoId && !seen.has(x.path));
      if (!extra.length) return false;
      remember(extra);
      const ord = [...order, ...extra.map((x) => x.path)];
      setOrder(ord);
      setBase((b) => [...b, ...extra.map((x) => x.path)]);
      loadAt(ord, index + 1);
      return true;
    } catch {
      return false;
    }
  }, [lookup, loadAt, remember]);

  const next = useCallback(
    async (auto = false) => {
      const { order, index, repeat } = live.current;
      if (!order.length) return;
      if (auto && repeat === 'one') return loadAt(order, index);
      if (index + 1 < order.length) return loadAt(order, index + 1);
      if (repeat === 'all') return loadAt(order, 0);
      // End of the queue: keep going with radio for YouTube tracks.
      if (await extendWithRadio()) return;
      if (!auto) loadAt(order, 0, false);
      else api.stop();
    },
    [loadAt, extendWithRadio],
  );

  const previous = useCallback(() => {
    const { order, index, state } = live.current;
    if (state.position > 3 || index <= 0) return api.seek(0);
    loadAt(order, index - 1);
  }, [loadAt]);

  const toggle = useCallback(() => {
    const { state, order, index } = live.current;
    if (state.playing) api.pause();
    else if (state.path) api.play();
    else if (order.length) loadAt(order, Math.max(0, index));
  }, [loadAt]);

  const setVolume = useCallback((v: number) => {
    setVolumeState(v);
    api.volume(v);
    try {
      localStorage.setItem('cw.volume', String(v));
    } catch {
      /* storage unavailable */
    }
  }, []);

  const toggleShuffle = useCallback(() => {
    const cur = live.current.order[live.current.index];
    if (!shuffle) {
      setOrder(shuffled(base, cur));
      setIndex(cur === undefined ? -1 : 0);
    } else {
      setOrder(base);
      setIndex(base.indexOf(cur));
    }
    setShuffle(!shuffle);
  }, [base, shuffle]);

  const cycleRepeat = useCallback(() => setRepeat((r) => (r === 'off' ? 'all' : r === 'all' ? 'one' : 'off')), []);
  const playFromQueue = useCallback((i: number) => loadAt(live.current.order, i), [loadAt]);
  const removeFromQueue = useCallback((i: number) => {
    setOrder((o) => o.filter((_, j) => j !== i));
  }, []);

  useEffect(() => {
    api.volume(volume);
    api.outputInfo().then(setOutput);
    const offs = [
      on<PlayerState>('player://state', (s) => {
        setState(s);
        if (s.path) setBuffering((b) => (b === s.path ? null : b));
      }),
      on<OutputInfo>('player://output', setOutput),
      on<StreamInfo>('player://stream', setStream),
      on<{ key: string; stage: string }>('player://buffering', (b) => {
        if (b.stage === 'idle') setBuffering((cur) => (cur && live.current.state.path === cur ? null : cur));
      }),
      on<string>('player://error', (e) => {
        setError(e);
        setBuffering(null);
      }),
      on<string | null>('player://ended', () => next(true)),
      on<string>('media://key', (key) => {
        if (key === 'toggle') toggle();
        else if (key === 'play') api.play();
        else if (key === 'pause' || key === 'stop') api.pause();
        else if (key === 'next') next();
        else if (key === 'previous') previous();
      }),
    ];
    return () => offs.forEach((f) => f());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (current) api.nowPlaying(current);
  }, [current]);
  useEffect(() => {
    if (state.path) api.mediaPlayback(state.playing, live.current.state.position);
  }, [state.playing, state.path]);

  const upNext = useMemo(
    () => order.slice(index + 1).map((p) => lookup(p)).filter((t): t is Track => !!t),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [order, index, lookup, knownVersion],
  );

  return {
    state,
    current,
    output,
    stream: stream && current && stream.key === current.path ? stream : null,
    buffering: buffering !== null,
    error,
    clearError: () => setError(null),
    upNext,
    queueStart: index + 1,
    shuffle,
    repeat,
    volume,
    playList,
    playRadio,
    playNext,
    addToQueue,
    playFromQueue,
    removeFromQueue,
    toggle,
    next: () => next(false),
    previous,
    seek: (s: number) => api.seek(s),
    setVolume,
    toggleShuffle,
    cycleRepeat,
  };
}

export type Player = ReturnType<typeof usePlayer>;
