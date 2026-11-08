import type { World } from './types';

/**
 * Coins, buddy levels and the item shop. Pure rules, unit-tested.
 *
 * Design: rewards follow effort, not just correctness. Every finished game pays, first-try
 * answers and daily streaks pay a little more, and nothing is ever taken away.
 */

export type Slot = 'hat' | 'face' | 'back';

export interface Wallet {
  coins: number;
  /** Lifetime coins earned; drives the buddy's level and never goes down. */
  xp: number;
  owned: string[];
  equipped: Partial<Record<Slot, string>>;
}

export interface Item {
  id: string;
  name: string;
  slot: Slot;
  emoji: string;
  price: number;
}

export const ITEMS: Item[] = [
  { id: 'cap', name: 'Cap', slot: 'hat', emoji: '🧢', price: 20 },
  { id: 'top-hat', name: 'Top hat', slot: 'hat', emoji: '🎩', price: 45 },
  { id: 'grad-cap', name: 'Smart cap', slot: 'hat', emoji: '🎓', price: 70 },
  { id: 'crown', name: 'Crown', slot: 'hat', emoji: '👑', price: 150 },
  { id: 'glasses', name: 'Glasses', slot: 'face', emoji: '👓', price: 25 },
  { id: 'shades', name: 'Sunglasses', slot: 'face', emoji: '🕶️', price: 55 },
  { id: 'sparkles', name: 'Sparkles', slot: 'face', emoji: '✨', price: 90 },
  { id: 'balloon', name: 'Balloon', slot: 'back', emoji: '🎈', price: 30 },
  { id: 'backpack', name: 'Backpack', slot: 'back', emoji: '🎒', price: 60 },
  { id: 'rocket', name: 'Rocket pack', slot: 'back', emoji: '🚀', price: 120 },
];

export const EMPTY_WALLET: Wallet = { coins: 0, xp: 0, owned: [], equipped: {} };

export interface Earnings {
  base: number;
  firstTryBonus: number;
  streakBonus: number;
  total: number;
}

/** Coins for one finished game. */
export function earn(rounds: number, firstTry: number, streakDays: number): Earnings {
  const base = 10;
  const firstTryBonus = 2 * Math.max(0, Math.min(firstTry, rounds));
  const streakBonus = streakDays >= 2 ? Math.min(streakDays, 7) : 0;
  return { base, firstTryBonus, streakBonus, total: base + firstTryBonus + streakBonus };
}

/** XP needed to reach each level grows gently: 80, 200, 360, 560, ... */
export function xpForLevel(level: number): number {
  if (level <= 1) return 0;
  return 20 * (level - 1) * (level + 2);
}

export function levelFromXp(xp: number): number {
  let level = 1;
  while (xpForLevel(level + 1) <= xp) level++;
  return level;
}

/** 0..1 progress towards the next level. */
export function levelProgress(xp: number): number {
  const level = levelFromXp(xp);
  const from = xpForLevel(level);
  const to = xpForLevel(level + 1);
  return (xp - from) / (to - from);
}

export function applyEarnings(wallet: Wallet, coins: number): Wallet {
  return { ...wallet, coins: wallet.coins + coins, xp: wallet.xp + coins };
}

export type BuyResult = { ok: true; wallet: Wallet } | { ok: false; reason: 'owned' | 'poor' | 'unknown' };

export function buy(wallet: Wallet, itemId: string): BuyResult {
  const item = ITEMS.find((i) => i.id === itemId);
  if (!item) return { ok: false, reason: 'unknown' };
  if (wallet.owned.includes(itemId)) return { ok: false, reason: 'owned' };
  if (wallet.coins < item.price) return { ok: false, reason: 'poor' };
  return {
    ok: true,
    wallet: {
      ...wallet,
      coins: wallet.coins - item.price,
      owned: [...wallet.owned, itemId],
      equipped: { ...wallet.equipped, [item.slot]: itemId },
    },
  };
}

/** Equips an owned item, or takes it off if it is already on. */
export function toggleEquip(wallet: Wallet, itemId: string): Wallet {
  const item = ITEMS.find((i) => i.id === itemId);
  if (!item || !wallet.owned.includes(itemId)) return wallet;
  const equipped = { ...wallet.equipped };
  if (equipped[item.slot] === itemId) delete equipped[item.slot];
  else equipped[item.slot] = itemId;
  return { ...wallet, equipped };
}

/** Cleans anything the host sends so a corrupt value can never break a session. */
export function normalizeWallet(raw: unknown): Wallet {
  if (typeof raw !== 'object' || raw === null) return { ...EMPTY_WALLET, equipped: {} };
  const r = raw as Record<string, unknown>;
  const num = (v: unknown) => (typeof v === 'number' && Number.isFinite(v) && v >= 0 ? Math.floor(v) : 0);
  const known = new Set(ITEMS.map((i) => i.id));
  const owned = Array.isArray(r.owned) ? r.owned.filter((x): x is string => typeof x === 'string' && known.has(x)) : [];
  const equipped: Partial<Record<Slot, string>> = {};
  if (typeof r.equipped === 'object' && r.equipped !== null) {
    for (const [slot, id] of Object.entries(r.equipped as Record<string, unknown>)) {
      const item = ITEMS.find((i) => i.id === id);
      if (item && item.slot === slot && owned.includes(item.id)) equipped[item.slot] = item.id;
    }
  }
  const coins = num(r.coins);
  return { coins, xp: Math.max(num(r.xp), coins), owned: [...new Set(owned)], equipped };
}

export const WORLDS: World[] = ['nuzo', 'lotli', 'embrix'];
