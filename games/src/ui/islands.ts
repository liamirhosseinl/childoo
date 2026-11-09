import { CHARACTER_NAMES, createMascot, setMood } from '../characters/mascots';
import type { Wallet } from '../engine/economy';
import type { World } from '../engine/types';

export interface IslandInfo {
  world: World;
  /** Games on this island the child can play at their age. */
  games: number;
  /** Average level across those games, rounded. */
  level: number;
  /** 0..1 progress through all levels on the island. */
  progress: number;
}

const ISLAND_NAMES: Record<World, string> = {
  nuzo: 'Number Planet',
  lotli: 'Word Lagoon',
  embrix: 'Courage Mountain',
};

const ISLAND_TAGS: Record<World, string> = {
  nuzo: 'Numbers, shapes & logic',
  lotli: 'Letters, words & reading',
  embrix: 'Move, breathe & remember',
};

/**
 * Hand-drawn islands (inline SVG): no image files to ship and crisp on any screen.
 * Each sits on its own sky so the three choices read apart at a glance.
 */
const ART: Record<World, string> = {
  nuzo: `
<svg viewBox="0 0 220 160" aria-hidden="true">
  <defs>
    <radialGradient id="pl" cx="35%" cy="30%" r="80%"><stop offset="0" stop-color="#9fd4ff"/><stop offset=".6" stop-color="#4d8fe8"/><stop offset="1" stop-color="#2a54b8"/></radialGradient>
  </defs>
  <g class="float">
    <ellipse cx="110" cy="86" rx="98" ry="22" fill="none" stroke="#ffc145" stroke-width="7" opacity=".9" transform="rotate(-12 110 86)"/>
    <circle cx="110" cy="80" r="52" fill="url(#pl)"/>
    <circle cx="90" cy="64" r="9" fill="#2a54b8" opacity=".35"/><circle cx="132" cy="96" r="13" fill="#2a54b8" opacity=".3"/><circle cx="124" cy="58" r="5" fill="#2a54b8" opacity=".35"/>
    <path d="M12 86 A98 22 0 0 0 208 86" fill="none" stroke="#ffc145" stroke-width="7" transform="rotate(-12 110 86)"/>
    <text x="110" y="92" font-family="Fredoka" font-weight="700" font-size="30" fill="#fff" text-anchor="middle">1 2 3</text>
  </g>
  <g fill="#fff"><circle cx="28" cy="26" r="2.5"/><circle cx="196" cy="34" r="2"/><circle cx="182" cy="132" r="2.5"/><circle cx="40" cy="128" r="2"/></g>
  <text x="188" y="62" font-size="26">🚀</text>
</svg>`,
  lotli: `
<svg viewBox="0 0 220 160" aria-hidden="true">
  <ellipse cx="110" cy="128" rx="104" ry="20" fill="#2fc4a6" opacity=".55"/>
  <g class="float">
    <path d="M30 120 C40 86 80 78 110 80 C150 80 186 92 192 120 Z" fill="#f5d38a"/>
    <path d="M44 120 C56 100 84 96 110 96 C142 96 170 104 178 120 Z" fill="#7bd389"/>
    <path d="M112 98 C110 70 116 44 124 30" stroke="#8b5a2b" stroke-width="6" fill="none" stroke-linecap="round"/>
    <path d="M124 30 C104 20 88 28 82 38 C100 32 112 34 124 30 Z M124 30 C140 14 160 16 168 26 C150 24 138 28 124 30 Z M124 30 C130 44 128 60 120 66 C122 50 120 40 124 30 Z M124 30 C112 40 98 58 98 68 C108 54 116 44 124 30 Z" fill="#2f9e5b"/>
  </g>
  <g font-family="Fredoka" font-weight="700" font-size="18" fill="#fff" text-anchor="middle">
    <circle cx="44" cy="54" r="15" fill="#ffffff33" stroke="#fff" stroke-width="2"/><text x="44" y="60">A</text>
    <circle cx="178" cy="62" r="13" fill="#ffffff33" stroke="#fff" stroke-width="2"/><text x="178" y="68">b</text>
    <circle cx="68" cy="24" r="11" fill="#ffffff33" stroke="#fff" stroke-width="2"/><text x="68" y="30">c</text>
  </g>
  <path d="M8 138 q14 -8 28 0 t28 0 t28 0 t28 0 t28 0 t28 0 t28 0" stroke="#fff" stroke-width="3" fill="none" opacity=".6"/>
</svg>`,
  embrix: `
<svg viewBox="0 0 220 160" aria-hidden="true">
  <ellipse cx="110" cy="134" rx="100" ry="16" fill="#000" opacity=".15"/>
  <g class="float">
    <path d="M20 134 L82 52 L104 76 L126 40 L200 134 Z" fill="#7a3f6e"/>
    <path d="M82 52 L94 68 L86 66 L78 72 L70 66 Z M126 40 L140 58 L130 56 L122 62 L114 56 Z" fill="#fff" opacity=".85"/>
    <path d="M20 134 L60 100 L92 120 L130 92 L200 134 Z" fill="#4c2a63"/>
    <path d="M126 40 L126 16" stroke="#fff" stroke-width="3"/>
    <path d="M126 16 L148 22 L126 28 Z" fill="#ffc145"/>
  </g>
  <g class="embers" fill="#ffb347"><circle cx="58" cy="40" r="3"/><circle cx="170" cy="58" r="2.5"/><circle cx="186" cy="30" r="2"/><circle cx="36" cy="78" r="2"/></g>
  <text x="150" y="128" font-size="24">🔥</text>
</svg>`,
};

/**
 * "Where to today?": the child chooses an island when play time is up. The choice itself is
 * useful: over weeks it shows parents what the child is drawn to.
 */
export function pickIsland(
  root: HTMLElement,
  islands: IslandInfo[],
  companion: World,
  wallet: Wallet,
  streakDays: number,
  onTap: () => void,
): Promise<World> {
  return new Promise((resolve) => {
    const layer = document.createElement('div');
    layer.className = 'islands';
    layer.innerHTML = `
      <header class="islands-top">
        <div class="islands-buddy"></div>
        <div class="islands-hello"><b>Time for a quick game!</b><span>Where do you want to go?</span></div>
        <div class="islands-stats">
          <div class="pill-stat coins"><span class="coin" aria-hidden="true"></span><b>${wallet.coins}</b></div>
          ${streakDays >= 2 ? `<div class="pill-stat streak">🔥 <b>${streakDays}</b></div>` : ''}
        </div>
      </header>
      <div class="island-list"></div>`;
    const list = layer.querySelector('.island-list') as HTMLElement;
    const buddy = createMascot(companion, wallet.equipped);
    (layer.querySelector('.islands-buddy') as HTMLElement).appendChild(buddy);

    islands.forEach((info, i) => {
      const card = document.createElement('button');
      card.type = 'button';
      card.className = `island island-${info.world}`;
      card.style.animationDelay = `${i * 90}ms`;
      card.innerHTML = `
        <div class="island-art">${ART[info.world]}</div>
        <div class="island-body">
          <b class="island-name">${ISLAND_NAMES[info.world]}</b>
          <span class="island-tag">${ISLAND_TAGS[info.world]}</span>
          <div class="island-meta">
            <span class="lv">Level ${info.level}</span>
            <span class="host">with ${CHARACTER_NAMES[info.world]}</span>
          </div>
          <div class="island-bar"><i style="width:${Math.round(info.progress * 100)}%"></i></div>
        </div>`;
      card.addEventListener('click', () => {
        onTap();
        card.classList.add('picked');
        setMood(buddy, 'cheer');
        list.querySelectorAll('.island').forEach((c) => c !== card && c.classList.add('faded'));
        window.setTimeout(() => {
          layer.classList.add('leaving');
          window.setTimeout(() => {
            layer.remove();
            resolve(info.world);
          }, 300);
        }, 550);
      });
      list.appendChild(card);
    });
    root.appendChild(layer);
  });
}
