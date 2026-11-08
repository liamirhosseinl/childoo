import { ITEMS, type Slot } from '../engine/economy';
import { clipFor } from './clips';
import type { Mood, World } from '../engine/types';

/**
 * Code-drawn stand-ins for Nuzo, Lotli and Embrix until the Rive files arrive.
 * Every character shares one face rig (eyes, happy eyes, five mouths) so moods are pure CSS.
 */

interface FaceRig {
  eyeY: number;
  eyeDx: number;
  eyeR: number;
  mouthY: number;
  cheekY: number;
}

function face(r: FaceRig, ink = '#1E2266'): string {
  const { eyeY, eyeDx, eyeR, mouthY, cheekY } = r;
  const lx = 100 - eyeDx;
  const rx = 100 + eyeDx;
  return `
  <g class="cheeks" fill="#FF7A59" opacity=".35">
    <ellipse cx="${lx - 14}" cy="${cheekY}" rx="11" ry="6"/>
    <ellipse cx="${rx + 14}" cy="${cheekY}" rx="11" ry="6"/>
  </g>
  <g class="eyes">
    <g class="eye" style="transform-origin:${lx}px ${eyeY}px">
      <ellipse cx="${lx}" cy="${eyeY}" rx="${eyeR}" ry="${eyeR * 1.15}" fill="${ink}"/>
      <circle class="glint" cx="${lx - eyeR * 0.35}" cy="${eyeY - eyeR * 0.45}" r="${eyeR * 0.36}" fill="#fff"/>
    </g>
    <g class="eye" style="transform-origin:${rx}px ${eyeY}px">
      <ellipse cx="${rx}" cy="${eyeY}" rx="${eyeR}" ry="${eyeR * 1.15}" fill="${ink}"/>
      <circle class="glint" cx="${rx - eyeR * 0.35}" cy="${eyeY - eyeR * 0.45}" r="${eyeR * 0.36}" fill="#fff"/>
    </g>
  </g>
  <g class="eyes-happy" fill="none" stroke="${ink}" stroke-width="5" stroke-linecap="round">
    <path d="M${lx - eyeR} ${eyeY + 3} Q${lx} ${eyeY - eyeR - 4} ${lx + eyeR} ${eyeY + 3}"/>
    <path d="M${rx - eyeR} ${eyeY + 3} Q${rx} ${eyeY - eyeR - 4} ${rx + eyeR} ${eyeY + 3}"/>
  </g>
  <g class="mouths" fill="none" stroke="${ink}" stroke-width="5" stroke-linecap="round">
    <path class="m-smile" d="M88 ${mouthY} Q100 ${mouthY + 11} 112 ${mouthY}"/>
    <path class="m-flat" d="M90 ${mouthY + 3} L109 ${mouthY - 1}"/>
    <circle class="m-o" cx="100" cy="${mouthY + 3}" r="5.5"/>
    <g class="m-open" stroke="none">
      <path d="M85 ${mouthY - 2} Q100 ${mouthY + 22} 115 ${mouthY - 2} Z" fill="${ink}"/>
      <ellipse cx="100" cy="${mouthY + 9}" rx="7" ry="3.5" fill="#FF7A59"/>
    </g>
    <ellipse class="m-talk" cx="100" cy="${mouthY + 3}" rx="7" ry="6" fill="${ink}" stroke="none"/>
  </g>`;
}

const NUZO = `
<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
  <defs>
    <radialGradient id="nz-body" cx="38%" cy="32%" r="78%">
      <stop offset="0" stop-color="#D4EEFF"/><stop offset=".55" stop-color="#8CC8F5"/><stop offset="1" stop-color="#5C9FE0"/>
    </radialGradient>
  </defs>
  <ellipse class="shadow" cx="100" cy="190" rx="52" ry="7" fill="#000" opacity=".16"/>
  <g class="body">
    <g class="slot slot-back"></g>
    <path d="M58 64 C52 44 60 34 70 36 C76 44 78 54 76 60 Z" fill="#B28CF5"/>
    <path d="M142 64 C148 44 140 34 130 36 C124 44 122 54 124 60 Z" fill="#B28CF5"/>
    <path d="M88 52 C90 36 110 36 112 52 Z" fill="#6FB2EA"/>
    <ellipse cx="74" cy="182" rx="17" ry="8" fill="#5C9FE0"/>
    <ellipse cx="126" cy="182" rx="17" ry="8" fill="#5C9FE0"/>
    <path d="M100 48 C146 48 168 82 168 122 C168 166 140 184 100 184 C60 184 32 166 32 122 C32 82 54 48 100 48 Z" fill="url(#nz-body)"/>
    <ellipse cx="68" cy="78" rx="17" ry="9" fill="#fff" opacity=".4" transform="rotate(-25 68 78)"/>
    ${face({ eyeY: 108, eyeDx: 26, eyeR: 12, mouthY: 136, cheekY: 128 })}
    <g class="slot slot-face"></g>
    <g class="slot slot-hat"></g>
  </g>
</svg>`;

const LOTLI = `
<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
  <defs>
    <radialGradient id="lt-body" cx="40%" cy="35%" r="75%">
      <stop offset="0" stop-color="#EEF8FF"/><stop offset=".6" stop-color="#BFE3F7"/><stop offset="1" stop-color="#8DC4E8"/>
    </radialGradient>
  </defs>
  <ellipse class="shadow" cx="100" cy="190" rx="50" ry="7" fill="#000" opacity=".16"/>
  <g class="body">
    <g class="slot slot-back"></g>
    <g class="gills" fill="#FF9EC8">
      <path d="M40 78 C14 60 10 44 20 40 C30 48 38 60 50 72 Z"/>
      <path d="M34 98 C8 96 0 84 6 76 C18 80 30 86 44 92 Z"/>
      <path d="M38 118 C14 128 4 122 6 112 C18 110 30 110 44 110 Z"/>
      <path d="M160 78 C186 60 190 44 180 40 C170 48 162 60 150 72 Z"/>
      <path d="M166 98 C192 96 200 84 194 76 C182 80 170 86 156 92 Z"/>
      <path d="M162 118 C186 128 196 122 194 112 C182 110 170 110 156 110 Z"/>
    </g>
    <ellipse cx="76" cy="180" rx="14" ry="8" fill="#8DC4E8"/>
    <ellipse cx="124" cy="180" rx="14" ry="8" fill="#8DC4E8"/>
    <path d="M100 150 C130 150 140 166 136 178 C126 186 74 186 64 178 C60 166 70 150 100 150 Z" fill="#BFE3F7"/>
    <ellipse cx="100" cy="170" rx="22" ry="10" fill="#EAF6FF"/>
    <ellipse cx="100" cy="102" rx="64" ry="54" fill="url(#lt-body)"/>
    <ellipse cx="74" cy="72" rx="16" ry="8" fill="#fff" opacity=".4" transform="rotate(-20 74 72)"/>
    ${face({ eyeY: 98, eyeDx: 30, eyeR: 10, mouthY: 122, cheekY: 116 })}
    <g class="slot slot-face"></g>
    <g class="slot slot-hat"></g>
  </g>
</svg>`;

const EMBRIX = `
<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
  <defs>
    <radialGradient id="em-body" cx="40%" cy="35%" r="75%">
      <stop offset="0" stop-color="#FFC28F"/><stop offset=".6" stop-color="#FF8A3D"/><stop offset="1" stop-color="#E5672A"/>
    </radialGradient>
  </defs>
  <ellipse class="shadow" cx="100" cy="190" rx="50" ry="7" fill="#000" opacity=".16"/>
  <g class="body">
    <g class="slot slot-back"></g>
    <path class="tail" d="M150 160 C182 160 190 128 172 114" fill="none" stroke="#E5672A" stroke-width="12" stroke-linecap="round"/>
    <path class="flame" d="M172 114 C160 100 170 86 176 80 C178 92 190 96 186 108 C184 114 178 116 172 114 Z" fill="#FFC145"/>
    <path d="M44 100 C16 86 8 60 22 52 C32 70 46 80 56 88 Z" fill="#FFC145" stroke="#E8A20F" stroke-width="2"/>
    <path d="M156 100 C184 86 192 60 178 52 C168 70 154 80 144 88 Z" fill="#FFC145" stroke="#E8A20F" stroke-width="2"/>
    <path d="M60 58 L52 22 L84 48 Z" fill="#E5672A"/>
    <path d="M140 58 L148 22 L116 48 Z" fill="#E5672A"/>
    <path d="M62 52 L58 32 L76 47 Z" fill="#FFD9B8"/>
    <path d="M138 52 L142 32 L124 47 Z" fill="#FFD9B8"/>
    <ellipse cx="76" cy="182" rx="15" ry="8" fill="#E5672A"/>
    <ellipse cx="124" cy="182" rx="15" ry="8" fill="#E5672A"/>
    <path d="M100 46 C144 46 160 80 160 116 C160 160 136 184 100 184 C64 184 40 160 40 116 C40 80 56 46 100 46 Z" fill="url(#em-body)"/>
    <path d="M100 128 C122 128 132 144 130 160 C126 176 74 176 70 160 C68 144 78 128 100 128 Z" fill="#FFE2C7"/>
    <path d="M84 52 C80 34 92 26 96 18 C100 30 108 24 112 14 C114 30 124 34 118 52 Z" fill="#FFB52E"/>
    <ellipse cx="72" cy="76" rx="15" ry="8" fill="#fff" opacity=".35" transform="rotate(-25 72 76)"/>
    ${face({ eyeY: 100, eyeDx: 24, eyeR: 10, mouthY: 124, cheekY: 118 })}
    <g class="slot slot-face"></g>
    <g class="slot slot-hat"></g>
  </g>
</svg>`;

const SVGS: Record<World, string> = { nuzo: NUZO, lotli: LOTLI, embrix: EMBRIX };

export const CHARACTER_NAMES: Record<World, string> = { nuzo: 'Nuzo', lotli: 'Lotli', embrix: 'Embrix' };

/** Where each accessory sits on each character, in the 200 × 200 drawing space. */
const SLOTS: Record<World, Record<Slot, { x: number; y: number; size: number; rotate: number }>> = {
  nuzo: { hat: { x: 100, y: 34, size: 58, rotate: -8 }, face: { x: 100, y: 110, size: 74, rotate: 0 }, back: { x: 166, y: 96, size: 50, rotate: 10 } },
  lotli: { hat: { x: 100, y: 40, size: 56, rotate: -8 }, face: { x: 100, y: 100, size: 80, rotate: 0 }, back: { x: 168, y: 132, size: 46, rotate: 10 } },
  embrix: { hat: { x: 100, y: 30, size: 58, rotate: -6 }, face: { x: 100, y: 102, size: 70, rotate: 0 }, back: { x: 40, y: 134, size: 48, rotate: -10 } },
};

export function createMascot(world: World, equipped: Partial<Record<Slot, string>> = {}): HTMLElement {
  const el = document.createElement('div');
  el.className = `mascot mascot-${world}`;
  el.dataset.mood = 'idle';
  el.dataset.world = world;
  const clip = clipFor(world, 'idle');
  if (clip) {
    el.classList.add('clip');
    el.innerHTML = `<img class="clip-img" src="${clip}" alt="" draggable="false">`;
    return el;
  }
  el.innerHTML = SVGS[world];
  dressMascot(el, world, equipped);
  return el;
}

/** Puts the equipped accessories on (or takes them off). */
export function dressMascot(el: HTMLElement, world: World, equipped: Partial<Record<Slot, string>>): void {
  (['hat', 'face', 'back'] as Slot[]).forEach((slot) => {
    const g = el.querySelector(`.slot-${slot}`);
    if (!g) return;
    const item = ITEMS.find((i) => i.id === equipped[slot]);
    if (!item) {
      g.innerHTML = '';
      return;
    }
    const p = SLOTS[world][slot];
    g.innerHTML = `<text x="${p.x}" y="${p.y}" font-size="${p.size}" text-anchor="middle" dominant-baseline="central" transform="rotate(${p.rotate} ${p.x} ${p.y})">${item.emoji}</text>`;
  });
}

export function setMood(el: HTMLElement, mood: Mood): void {
  el.dataset.mood = mood;
  if (el.classList.contains('clip')) {
    const img = el.querySelector('.clip-img') as HTMLImageElement | null;
    const src = clipFor(el.dataset.world as World, mood);
    if (img && src && !img.src.endsWith(src)) img.src = src;
    return;
  }
  // Restart one-shot CSS animations when the same mood fires twice in a row.
  if (mood === 'happy' || mood === 'oops' || mood === 'cheer') {
    el.classList.remove('replay');
    void el.offsetWidth;
    el.classList.add('replay');
  }
}
