import type { Slot } from '../engine/economy';
import { ITEMS } from '../engine/economy';
import type { Mood, World } from '../engine/types';
import { CLIP_FOR_MOOD, clipCanvas, clipInfo, clipUrl, type ClipState } from './clips';

/**
 * Plays a character's animated clips with smooth hand-offs:
 * looping states (idle, talk) are the resting pose; one-shots (happy, celebrate, oops, …)
 * play once and then settle back. Accessories follow the head using the clip's track.
 */

/** Where each accessory sits relative to the head: offsets and size in head widths. */
const ANCHOR: Record<Slot, { dx: number; dy: number; size: number; rotate: number }> = {
  hat: { dx: 0, dy: -0.06, size: 0.54, rotate: -8 },
  face: { dx: 0, dy: 0.3, size: 0.66, rotate: 0 },
  back: { dx: 0.44, dy: 1.0, size: 0.4, rotate: 12 },
};

const LOOPING: ClipState[] = ['idle', 'talk', 'walk'];

/** A one-shot always gets at least this long before a resting pose may take over. */
const MIN_ONE_SHOT_MS = 1700;

interface PlayerState {
  world: World;
  rest: ClipState;
  current: ClipState;
  front: HTMLImageElement;
  back: HTMLImageElement;
  acc: HTMLElement;
  started: number;
  oneShotTimer: number;
  restTimer: number;
  raf: number;
  serial: number;
  /** The clip being played; also guards against stale loads. */
  playing: ClipState | null;
}

const players = new WeakMap<HTMLElement, PlayerState>();

export function createClipMascot(world: World, equipped: Partial<Record<Slot, string>>): HTMLElement {
  const el = document.createElement('div');
  el.className = `mascot mascot-${world} clip`;
  el.dataset.mood = 'idle';
  el.dataset.world = world;
  const { w, h, box } = clipCanvas(world);
  el.style.setProperty('--clip-box', `${w} / ${Math.round(h * box)}`);
  el.style.setProperty('--clip-over', String(1 / box));
  el.innerHTML = `
    <div class="clip-stage">
      <img class="clip-img" alt="" draggable="false" decoding="async">
      <img class="clip-img" alt="" draggable="false" decoding="async">
      <div class="clip-acc" aria-hidden="true"></div>
    </div>`;
  const [front, back] = Array.from(el.querySelectorAll<HTMLImageElement>('.clip-img'));
  front.classList.add('on');
  const st: PlayerState = {
    world,
    rest: 'idle',
    current: 'idle',
    front,
    back,
    acc: el.querySelector('.clip-acc') as HTMLElement,
    started: performance.now(),
    oneShotTimer: 0,
    restTimer: 0,
    raf: 0,
    serial: 0,
    playing: null,
  };
  players.set(el, st);
  front.src = clipUrl(world, 'idle');
  dressClipMascot(el, equipped);
  // A poke makes the buddy react; kids love it.
  el.addEventListener('pointerdown', () => {
    if (el.closest('button')) return;
    if (!LOOPING.includes(st.playing ?? st.current)) return;
    if (!clipInfo(world, 'tap')) return;
    play(el, st, 'tap');
  });
  tick(el, st);
  return el;
}

export function isClipMascot(el: HTMLElement): boolean {
  return players.has(el);
}

export function setClipMood(el: HTMLElement, mood: Mood): void {
  const st = players.get(el);
  if (!st) return;
  let state = CLIP_FOR_MOOD[mood];
  if (!clipInfo(st.world, state)) state = state === 'celebrate' ? 'happy' : 'idle';
  if (!clipInfo(st.world, state)) state = 'idle';
  if (LOOPING.includes(state)) {
    st.rest = state;
    window.clearTimeout(st.restTimer);
    const active = st.playing ?? st.current;
    if (LOOPING.includes(active)) {
      if (active !== state) play(el, st, state);
      return;
    }
    // A one-shot is on: let it land its moment, then settle into the resting pose.
    const serial = st.serial;
    const since = st.playing === st.current ? performance.now() - st.started : 0;
    const wait = Math.max(0, MIN_ONE_SHOT_MS - since);
    st.restTimer = window.setTimeout(() => {
      if (serial === st.serial) play(el, st, st.rest);
    }, wait);
    return;
  }
  play(el, st, state);
}

function play(el: HTMLElement, st: PlayerState, state: ClipState): void {
  const info = clipInfo(st.world, state);
  if (!info) return;
  window.clearTimeout(st.oneShotTimer);
  window.clearTimeout(st.restTimer);
  st.serial += 1;
  const serial = st.serial;
  st.playing = state;
  el.dataset.clip = state;
  const next = st.back;
  // A fresh URL restarts the animation from its first frame (shared images share their timeline).
  const url = `${clipUrl(st.world, state)}?p=${serial}`;
  const swap = () => {
    if (serial !== st.serial) return;
    next.classList.add('on');
    st.front.classList.remove('on');
    st.back = st.front;
    st.front = next;
    st.current = state;
    st.started = performance.now();
    if (!info.loop) {
      st.oneShotTimer = window.setTimeout(() => {
        if (serial === st.serial) play(el, st, st.rest);
      }, info.ms);
    }
  };
  next.onload = () => {
    if (typeof next.decode === 'function') next.decode().then(swap, swap);
    else swap();
  };
  next.onerror = () => {
    if (serial === st.serial && state !== 'idle') play(el, st, 'idle');
  };
  next.src = url;
}

/** Moves the accessory layer along the head track of the clip on screen. */
function tick(el: HTMLElement, st: PlayerState): void {
  const step = () => {
    if (!el.isConnected) {
      // Wait to be attached, but stop once it has been removed after being shown.
      if (el.dataset.shown) return;
      st.raf = window.requestAnimationFrame(step);
      return;
    }
    el.dataset.shown = '1';
    const info = clipInfo(st.world, st.current);
    if (info && info.track.length && st.acc.childElementCount) {
      const n = info.track.length;
      const frame = Math.floor(((performance.now() - st.started) / 1000) * info.fps);
      const i = info.loop ? frame % n : Math.min(n - 1, frame);
      const [x, y, s] = info.track[i];
      st.acc.style.setProperty('--hx', String(x));
      st.acc.style.setProperty('--hy', String(y));
      st.acc.style.setProperty('--hs', String(s));
    }
    st.raf = window.requestAnimationFrame(step);
  };
  st.raf = window.requestAnimationFrame(step);
}

export function dressClipMascot(el: HTMLElement, equipped: Partial<Record<Slot, string>>): void {
  const st = players.get(el);
  if (!st) return;
  st.acc.innerHTML = '';
  (['back', 'hat', 'face'] as Slot[]).forEach((slot) => {
    const item = ITEMS.find((i) => i.id === equipped[slot]);
    if (!item) return;
    const a = ANCHOR[slot];
    const span = document.createElement('span');
    span.className = `acc acc-${slot}`;
    span.textContent = item.emoji;
    span.style.setProperty('--dx', String(a.dx));
    span.style.setProperty('--dy', String(a.dy));
    span.style.setProperty('--size', String(a.size));
    span.style.setProperty('--rot', `${a.rotate}deg`);
    st.acc.appendChild(span);
  });
  el.classList.toggle('dressed', st.acc.childElementCount > 0);
}
