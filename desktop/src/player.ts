// Queue and playback state. The Rust engine plays one file at a time; the
// queue, shuffle and repeat live here and drive it.

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, on, type OutputInfo, type PlayerState, type Track } from './api';

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
    const v = Number(localStorage.getItem(k));
    return Number.isFinite(v) && localStorage.getItem(k) !== null ? v : d;
  } catch {
    return d;
  }
};

export function usePlayer(tracksByPath: Map<string, Track>) {
  const [state, setState] = useState<PlayerState>({ path: null, playing: false, position: 0, duration: 0 });
  const [output, setOutput] = useState<OutputInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<string[]>([]); // play order (shuffled or not)
  const [base, setBase] = useState<string[]>([]); // original order, for un-shuffle
  const [index, setIndex] = useState(-1);
  const [shuffle, setShuffle] = useState(false);
  const [repeat, setRepeat] = useState<Repeat>('off');
  const [volume, setVolumeState] = useState(() => readNum('cw.volume', 0.8));

  // Refs so event handlers registered once always see fresh values.
  const live = useRef({ order, index, repeat, state });
  live.current = { order, index, repeat, state };

  const current = state.path ? tracksByPath.get(state.path) ?? null : null;

  const loadAt = useCallback((list: string[], i: number, autoplay = true) => {
    if (i < 0 || i >= list.length) return;
    setIndex(i);
    setError(null);
    api.load(list[i], autoplay);
  }, []);

  /** Plays `list` starting at `start`; this becomes the new queue. */
  const playList = useCallback(
    (list: Track[], start = 0) => {
      const paths = list.map((t) => t.path);
      setBase(paths);
      const ord = shuffle ? shuffled(paths, paths[start]) : paths;
      setOrder(ord);
      loadAt(ord, shuffle ? 0 : start);
    },
    [shuffle, loadAt],
  );

  const next = useCallback(
    (auto = false) => {
      const { order, index, repeat } = live.current;
      if (!order.length) return;
      if (auto && repeat === 'one') return loadAt(order, index);
      if (index + 1 < order.length) return loadAt(order, index + 1);
      if (repeat === 'all' || !auto) return loadAt(order, 0, auto || live.current.state.playing);
      api.stop();
    },
    [loadAt],
  );

  const previous = useCallback(() => {
    const { order, index, state } = live.current;
    if (state.position > 3 || index <= 0) return api.seek(0);
    loadAt(order, index - 1);
  }, [loadAt]);

  const toggle = useCallback(() => {
    const { state, order } = live.current;
    if (state.playing) api.pause();
    else if (state.path) api.play();
    else if (order.length) loadAt(order, 0);
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

  // Backend events.
  useEffect(() => {
    api.volume(volume);
    api.outputInfo().then(setOutput);
    const offs = [
      on<PlayerState>('player://state', setState),
      on<OutputInfo>('player://output', setOutput),
      on<string>('player://error', setError),
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

  // Windows media flyout: metadata on track change, state on play/pause.
  useEffect(() => {
    if (current) api.nowPlaying(current);
  }, [current]);
  useEffect(() => {
    if (state.path) api.mediaPlayback(state.playing, live.current.state.position);
  }, [state.playing, state.path]);

  const upNext = useMemo(
    () => order.slice(index + 1).map((p) => tracksByPath.get(p)).filter((t): t is Track => !!t),
    [order, index, tracksByPath],
  );

  return {
    state,
    current,
    output,
    error,
    clearError: () => setError(null),
    upNext,
    queueStart: index + 1,
    shuffle,
    repeat,
    volume,
    playList,
    playFromQueue,
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
