/** Small seeded PRNG (mulberry32) so every session is reproducible from its seed. */
export interface Rng {
  next(): number;
  int(min: number, max: number): number;
  pick<T>(items: readonly T[]): T;
  shuffle<T>(items: readonly T[]): T[];
  chance(p: number): boolean;
}

export function createRng(seed: number): Rng {
  let s = seed >>> 0;
  const next = () => {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = s;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  const int = (min: number, max: number) => min + Math.floor(next() * (max - min + 1));
  return {
    next,
    int,
    pick: (items) => items[Math.floor(next() * items.length)],
    shuffle: (items) => {
      const a = items.slice();
      for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(next() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]];
      }
      return a;
    },
    chance: (p) => next() < p,
  };
}

/** Distinct numeric distractors near [answer], never negative, never equal to the answer. */
export function numberOptions(rng: Rng, answer: number, count = 4, spread = 3, min = 0): number[] {
  const set = new Set<number>([answer]);
  let guard = 0;
  while (set.size < count && guard++ < 200) {
    const d = rng.int(-spread, spread);
    const v = answer + d;
    if (v >= min && v !== answer) set.add(v);
  }
  let filler = answer + spread + 1;
  while (set.size < count) set.add(filler++);
  return rng.shuffle([...set]);
}
