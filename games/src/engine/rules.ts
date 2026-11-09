import type { Game, HostConfig, RoundRecord } from './types';
import type { Rng } from './rng';

/**
 * Pure decision rules, kept apart from the DOM so they are unit-tested.
 */

/** Starting level for a game when the child has no history with it. */
export function defaultLevel(age: number, maxLevel: number): number {
  const byAge = age <= 5 ? 1 : age <= 7 ? 2 : age <= 9 ? 3 : 4;
  return clamp(byAge, 1, maxLevel);
}

/**
 * Guessing: a round with two or more wrong taps fired in quick succession,
 * or more wrong taps than rounds overall. Thoughtful mistakes are slower than this.
 */
export function looksLikeGuessing(records: RoundRecord[], quickMs = 1200): boolean {
  const totalWrong = records.reduce((n, r) => n + r.wrongAt.length, 0);
  if (records.length > 0 && totalWrong > records.length * 1.5) return true;
  return records.some((r) => {
    if (r.wrongAt.length < 2) return false;
    const taps = [...r.wrongAt, r.solvedAt];
    let quick = 0;
    for (let i = 1; i < taps.length; i++) if (taps[i] - taps[i - 1] < quickMs) quick++;
    return quick >= 2 || r.wrongAt[0] < 700;
  });
}

/**
 * Next level: up when every round was right first time at a steady pace,
 * down when fewer than half were, otherwise unchanged.
 */
export function nextLevel(level: number, maxLevel: number, records: RoundRecord[], guessing: boolean): number {
  if (records.length === 0) return level;
  if (guessing) return clamp(level, 1, maxLevel);
  const firstTry = records.filter((r) => r.wrongAt.length === 0).length / records.length;
  const avgMs = records.reduce((n, r) => n + r.solvedAt, 0) / records.length;
  if (firstTry === 1 && avgMs < 9000) return clamp(level + 1, 1, maxLevel);
  // Most right first time and at a good pace: still time to move on.
  if (firstTry >= 2 / 3 && avgMs < 7000) return clamp(level + 1, 1, maxLevel);
  if (firstTry < 0.5) return clamp(level - 1, 1, maxLevel);
  return level;
}

/**
 * Picks the next game: right age, not one of the last three, and a different world from the last game
 * so a session mixes math, words and movement.
 */
export function pickGame(games: Game[], config: HostConfig, rng: Rng): Game {
  if (config.forceGame) {
    const forced = games.find((g) => g.id === config.forceGame);
    if (forced) return forced;
  }
  const age = config.age ?? 6;
  const recent = config.recent ?? [];
  const eligible = games.filter((g) => age >= g.minAge && age <= g.maxAge);
  const pool = eligible.length > 0 ? eligible : games;
  const lastWorld = pool.find((g) => g.id === recent[0])?.world ?? games.find((g) => g.id === recent[0])?.world;

  const fresh = pool.filter((g) => !recent.slice(0, 3).includes(g.id));
  const otherWorld = fresh.filter((g) => g.world !== lastWorld);
  const candidates = otherWorld.length > 0 ? otherWorld : fresh.length > 0 ? fresh : pool;
  return rng.pick(candidates);
}

export function clamp(v: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, v));
}
