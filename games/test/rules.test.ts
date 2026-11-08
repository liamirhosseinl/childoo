import { describe, expect, it } from 'vitest';
import { createRng, numberOptions } from '../src/engine/rng';
import { defaultLevel, looksLikeGuessing, nextLevel, pickGame } from '../src/engine/rules';
import { GAMES } from '../src/games';

describe('numberOptions', () => {
  it('always contains the answer, is distinct and never below min', () => {
    const rng = createRng(1);
    for (let i = 0; i < 2000; i++) {
      const answer = rng.int(0, 30);
      const opts = numberOptions(rng, answer, 4, 3, 0);
      expect(opts).toContain(answer);
      expect(new Set(opts).size).toBe(4);
      expect(Math.min(...opts)).toBeGreaterThanOrEqual(0);
    }
  });
});

describe('guessing detection', () => {
  it('flags rapid repeated wrong taps', () => {
    expect(looksLikeGuessing([{ wrongAt: [400, 800, 1100], solvedAt: 1500 }])).toBe(true);
  });
  it('does not flag a single thoughtful mistake', () => {
    expect(looksLikeGuessing([{ wrongAt: [4000], solvedAt: 7000 }, { wrongAt: [], solvedAt: 5000 }])).toBe(false);
  });
});

describe('adaptive level', () => {
  const clean = { wrongAt: [], solvedAt: 4000 };
  const messy = { wrongAt: [3000, 6000], solvedAt: 9000 };
  it('goes up after a clean, steady session', () => expect(nextLevel(2, 4, [clean, clean, clean], false)).toBe(3));
  it('goes down when most rounds were missed', () => expect(nextLevel(2, 4, [messy, messy, clean], false)).toBe(1));
  it('never leaves its range', () => {
    expect(nextLevel(4, 4, [clean, clean], false)).toBe(4);
    expect(nextLevel(1, 4, [messy, messy], false)).toBe(1);
  });
  it('does not reward guessing', () => expect(nextLevel(2, 4, [clean, clean], true)).toBe(2));
  it('starts by age', () => {
    expect(defaultLevel(4, 4)).toBe(1);
    expect(defaultLevel(7, 4)).toBe(2);
    expect(defaultLevel(10, 2)).toBe(2);
  });
});

describe('planner', () => {
  it('only picks games for the child age', () => {
    const rng = createRng(3);
    for (let i = 0; i < 300; i++) {
      const g = pickGame(GAMES, { age: 4 }, rng);
      expect(g.minAge).toBeLessThanOrEqual(4);
    }
  });
  it('avoids the last three games and the last world', () => {
    const rng = createRng(5);
    const recent = ['star-counter', 'rocket-fuel', 'space-scale'];
    for (let i = 0; i < 300; i++) {
      const g = pickGame(GAMES, { age: 7, recent }, rng);
      expect(recent).not.toContain(g.id);
      expect(g.world).not.toBe('nuzo');
    }
  });
  it('honours a forced game', () => {
    expect(pickGame(GAMES, { forceGame: 'dragon-breath' }, createRng(1)).id).toBe('dragon-breath');
  });
  it('has unique ids', () => {
    expect(new Set(GAMES.map((g) => g.id)).size).toBe(GAMES.length);
  });
});
