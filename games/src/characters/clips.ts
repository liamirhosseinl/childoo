import type { Mood, World } from '../engine/types';

/**
 * Animated character clips (transparent animated WebP), one per character and state, in
 * public/characters/<world>/<state>.webp and described in public/characters/manifest.json.
 *
 * Each state carries its length, whether it loops, and a per-frame head track
 * ([x, y, size] as fractions of the clip canvas) so accessories can ride along with the animation.
 */
export type ClipState =
  | 'idle'
  | 'talk'
  | 'hello'
  | 'happy'
  | 'celebrate'
  | 'tap'
  | 'oops'
  | 'magic'
  | 'walk';

interface StateInfo {
  ms: number;
  loop: boolean;
  fps: number;
  /** One entry per frame: head centre x, head top y, head width; all as fractions of the canvas. */
  track: [number, number, number][];
}

interface CharacterClips {
  w: number;
  h: number;
  /** Share of the canvas height (from the bottom) the standing pose needs; the rest is jump room. */
  box: number;
  states: Partial<Record<ClipState, StateInfo>>;
}

type Manifest = Partial<Record<World, CharacterClips>>;

let manifest: Manifest = {};

export async function loadClipManifest(): Promise<void> {
  try {
    const res = await fetch('characters/manifest.json', { cache: 'no-cache' });
    if (res.ok) manifest = (await res.json()) as Manifest;
  } catch {
    manifest = {};
  }
}

export function hasClips(world: World): boolean {
  return Boolean(manifest[world]?.states.idle);
}

export function clipInfo(world: World, state: ClipState): StateInfo | null {
  return manifest[world]?.states[state] ?? null;
}

export function clipCanvas(world: World): { w: number; h: number; box: number } {
  const c = manifest[world];
  return { w: c?.w ?? 400, h: c?.h ?? 500, box: c?.box ?? 1 };
}

export function clipUrl(world: World, state: ClipState): string {
  return `characters/${world}/${state}.webp`;
}

/** Which clip plays for a mood. Looping moods become the resting state; the rest play once. */
export const CLIP_FOR_MOOD: Record<Mood, ClipState> = {
  idle: 'idle',
  thinking: 'idle',
  talk: 'talk',
  happy: 'happy',
  oops: 'oops',
  cheer: 'celebrate',
  hello: 'hello',
  tap: 'tap',
  magic: 'magic',
  walk: 'walk',
};
