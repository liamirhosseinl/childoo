import type { Mood, World } from '../engine/types';

/**
 * Optional animated character clips (transparent animated WebP), one per character and state,
 * in public/characters/<world>/<state>.webp and listed in public/characters/manifest.json.
 * When a character has clips, they replace the code-drawn stand-in automatically.
 */
type Manifest = Partial<Record<World, string[]>>;

let manifest: Manifest = {};

/** State file names follow the animation spec. */
const STATE_FOR_MOOD: Record<Mood, string> = {
  idle: 'idle',
  thinking: 'thinking',
  talk: 'talk',
  happy: 'happy',
  oops: 'oops',
  cheer: 'celebrate',
};

export async function loadClipManifest(): Promise<void> {
  try {
    const res = await fetch('characters/manifest.json', { cache: 'no-cache' });
    if (res.ok) manifest = (await res.json()) as Manifest;
  } catch {
    manifest = {};
  }
}

/** URL of the clip for this mood, falling back to idle; null when the character has no clips. */
export function clipFor(world: World, mood: Mood): string | null {
  const states = manifest[world];
  if (!states || !states.includes('idle')) return null;
  const wanted = STATE_FOR_MOOD[mood];
  const state = states.includes(wanted) ? wanted : 'idle';
  return `characters/${world}/${state}.webp`;
}
