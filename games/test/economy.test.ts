import { describe, expect, it } from 'vitest';
import { EMPTY_WALLET, applyEarnings, buy, earn, levelFromXp, levelProgress, normalizeWallet, toggleEquip, xpForLevel } from '../src/engine/economy';

describe('earnings', () => {
  it('always pays for finishing', () => expect(earn(3, 0, 1).total).toBe(10));
  it('pays more for first-try answers and streaks', () => {
    expect(earn(3, 3, 1).total).toBe(16);
    expect(earn(3, 3, 4).total).toBe(20);
  });
  it('caps the streak bonus', () => expect(earn(3, 0, 30).streakBonus).toBe(7));
});

describe('levels', () => {
  it('starts at level 1 and grows', () => {
    expect(levelFromXp(0)).toBe(1);
    expect(levelFromXp(xpForLevel(2))).toBe(2);
    expect(levelFromXp(xpForLevel(5) - 1)).toBe(4);
  });
  it('reports progress between 0 and 1', () => {
    for (let xp = 0; xp < 2000; xp += 37) {
      const p = levelProgress(xp);
      expect(p).toBeGreaterThanOrEqual(0);
      expect(p).toBeLessThan(1);
    }
  });
});

describe('shop', () => {
  const rich = applyEarnings(EMPTY_WALLET, 200);
  it('buys and equips', () => {
    const r = buy(rich, 'crown');
    expect(r.ok).toBe(true);
    if (r.ok) {
      expect(r.wallet.coins).toBe(50);
      expect(r.wallet.equipped.hat).toBe('crown');
      expect(r.wallet.xp).toBe(200);
    }
  });
  it('refuses when too poor or already owned', () => {
    expect(buy(EMPTY_WALLET, 'cap')).toEqual({ ok: false, reason: 'poor' });
    const r = buy(rich, 'cap');
    if (r.ok) expect(buy(r.wallet, 'cap')).toEqual({ ok: false, reason: 'owned' });
  });
  it('toggles an owned item on and off', () => {
    const r = buy(rich, 'cap');
    if (!r.ok) throw new Error('should buy');
    const off = toggleEquip(r.wallet, 'cap');
    expect(off.equipped.hat).toBeUndefined();
    expect(toggleEquip(off, 'cap').equipped.hat).toBe('cap');
  });
  it('cleans corrupt wallets', () => {
    const w = normalizeWallet({ coins: -5, xp: 'x', owned: ['cap', 'nope', 'cap'], equipped: { hat: 'cap', face: 'crown' } });
    expect(w).toEqual({ coins: 0, xp: 0, owned: ['cap'], equipped: { hat: 'cap' } });
    expect(normalizeWallet(null)).toEqual({ coins: 0, xp: 0, owned: [], equipped: {} });
  });
});
