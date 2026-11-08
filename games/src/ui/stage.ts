import { CHARACTER_NAMES, createMascot, setMood } from '../characters/mascots';
import type { Choice, ChooseOptions, Mood, World } from '../engine/types';

/**
 * The fixed screen layout every game plays inside:
 * top bar (world, title, stars) · host character with speech bubble · play area · answer tray.
 */
export class Stage {
  readonly root: HTMLElement;
  readonly play: HTMLElement;
  private readonly tray: HTMLElement;
  private readonly title: HTMLElement;
  private readonly worldChip: HTMLElement;
  private readonly stars: HTMLElement;
  private readonly bubble: HTMLElement;
  private readonly feedbackEl: HTMLElement;
  private readonly hero: HTMLElement;
  private mascotEl: HTMLElement | null = null;
  private moodTimer = 0;
  /** Set by the session: replays the last spoken line. */
  onReplayRequest: () => void = () => {};

  constructor(mount: HTMLElement) {
    mount.innerHTML = `
      <div class="screen">
        <div class="sky" aria-hidden="true"></div>
        <header class="topbar">
          <div class="chip world-chip"><span class="chip-dot"></span><span class="world-name"></span></div>
          <div class="game-title"></div>
          <div class="stars" aria-label="progress"></div>
        </header>
        <section class="hero">
          <div class="mascot-slot"></div>
          <div class="bubble-wrap">
            <div class="bubble" role="status" aria-live="polite"></div>
            <button class="replay-btn" type="button" aria-label="Hear it again">
              <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path fill="currentColor" d="M3 10v4h4l5 4V6L7 10H3zm13.5 2a4.5 4.5 0 0 0-2.5-4v8a4.5 4.5 0 0 0 2.5-4zM14 3.2v2.1a7 7 0 0 1 0 13.4v2.1a9 9 0 0 0 0-17.6z"/></svg>
            </button>
          </div>
        </section>
        <div class="feedback" aria-live="polite"></div>
        <main class="play"></main>
        <footer class="tray"></footer>
      </div>`;
    this.root = mount.querySelector('.screen') as HTMLElement;
    this.play = mount.querySelector('.play') as HTMLElement;
    this.tray = mount.querySelector('.tray') as HTMLElement;
    this.title = mount.querySelector('.game-title') as HTMLElement;
    this.worldChip = mount.querySelector('.world-name') as HTMLElement;
    this.stars = mount.querySelector('.stars') as HTMLElement;
    this.bubble = mount.querySelector('.bubble') as HTMLElement;
    this.feedbackEl = mount.querySelector('.feedback') as HTMLElement;
    this.hero = mount.querySelector('.hero') as HTMLElement;
    (mount.querySelector('.replay-btn') as HTMLElement).addEventListener('click', () => this.onReplayRequest());
    this.makeSky();
  }

  setWorld(world: World, title: string): void {
    this.root.dataset.world = world;
    this.title.textContent = title;
    this.worldChip.textContent = WORLD_NAMES[world];
    const slot = this.hero.querySelector('.mascot-slot') as HTMLElement;
    slot.innerHTML = '';
    this.mascotEl = createMascot(world);
    this.mascotEl.setAttribute('aria-label', CHARACTER_NAMES[world]);
    slot.appendChild(this.mascotEl);
    this.mascot('idle');
  }

  setStars(total: number, done: number): void {
    const current = this.stars.children.length;
    if (current !== total) {
      this.stars.innerHTML = '';
      for (let i = 0; i < total; i++) {
        const s = document.createElement('span');
        s.className = 'star';
        s.innerHTML = STAR_SVG;
        this.stars.appendChild(s);
      }
    }
    Array.from(this.stars.children).forEach((s, i) => s.classList.toggle('on', i < done));
  }

  prompt(text: string): void {
    this.bubble.textContent = text;
    this.bubble.classList.remove('pop');
    void this.bubble.offsetWidth;
    this.bubble.classList.add('pop');
  }

  feedback(text: string, kind: 'good' | 'try' | 'info'): void {
    this.feedbackEl.textContent = text;
    this.feedbackEl.dataset.kind = kind;
    this.feedbackEl.classList.remove('show');
    void this.feedbackEl.offsetWidth;
    this.feedbackEl.classList.add('show');
  }

  clearFeedback(): void {
    this.feedbackEl.classList.remove('show');
  }

  mascot(mood: Mood, holdMs = 0): void {
    if (!this.mascotEl) return;
    window.clearTimeout(this.moodTimer);
    setMood(this.mascotEl, mood);
    if (holdMs > 0) {
      this.moodTimer = window.setTimeout(() => this.mascotEl && setMood(this.mascotEl, 'thinking'), holdMs);
    }
  }

  clearRound(): void {
    this.play.innerHTML = '';
    this.tray.innerHTML = '';
    this.play.className = 'play';
    this.clearFeedback();
  }

  /** Big chunky answer tiles. Calls back on every tap; the caller decides what is correct. */
  renderChoices<T>(
    choices: Choice<T>[],
    options: ChooseOptions<T>,
    onTap: (choice: Choice<T>, el: HTMLButtonElement) => void,
  ): HTMLButtonElement[] {
    this.tray.innerHTML = '';
    const layout = options.layout ?? (choices.length === 3 ? 'row' : 'grid');
    this.tray.dataset.layout = layout;
    return choices.map((c, i) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = `tile tile-c${i % 4} tile-${c.kind ?? 'number'}`;
      b.innerHTML = `<span class="tile-face">${escapeHtml(c.label)}</span>`;
      b.addEventListener('click', () => {
        if (b.disabled) return;
        onTap(c, b);
      });
      this.tray.appendChild(b);
      return b;
    });
  }

  celebrate(title: string, sub: string, world: World): void {
    const layer = document.createElement('div');
    layer.className = 'celebrate';
    layer.innerHTML = `<div class="celebrate-card"><div class="celebrate-mascot"></div><h1>${escapeHtml(title)}</h1><p>${escapeHtml(sub)}</p></div>`;
    const m = createMascot(world);
    setMood(m, 'cheer');
    (layer.querySelector('.celebrate-mascot') as HTMLElement).appendChild(m);
    this.root.appendChild(layer);
    confetti(layer);
  }

  private makeSky(): void {
    const sky = this.root.querySelector('.sky') as HTMLElement;
    for (let i = 0; i < 28; i++) {
      const s = document.createElement('i');
      // Deterministic placement so screenshots stay stable.
      const x = (i * 37) % 100;
      const y = (i * 53) % 100;
      s.style.left = `${x}%`;
      s.style.top = `${y}%`;
      s.style.animationDelay = `${(i % 7) * 0.45}s`;
      s.style.transform = `scale(${0.6 + (i % 4) * 0.25})`;
      sky.appendChild(s);
    }
  }
}

const WORLD_NAMES: Record<World, string> = {
  nuzo: 'Number Planet',
  lotli: 'Word Lagoon',
  embrix: 'Courage Mountain',
};

const STAR_SVG =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2.5l2.9 6 6.6.8-4.9 4.5 1.3 6.5L12 17.1l-5.9 3.2 1.3-6.5L2.5 9.3l6.6-.8z"/></svg>';

const CONFETTI_COLORS = ['#FFC145', '#FF7A59', '#2FC4A6', '#4DA8FF', '#9B7BFF', '#ffffff'];

function confetti(host: HTMLElement): void {
  for (let i = 0; i < 70; i++) {
    const p = document.createElement('i');
    p.className = 'confetti';
    p.style.left = `${(i * 13.7) % 100}%`;
    p.style.background = CONFETTI_COLORS[i % CONFETTI_COLORS.length];
    p.style.animationDelay = `${(i % 10) * 0.08}s`;
    p.style.animationDuration = `${1.8 + (i % 5) * 0.25}s`;
    p.style.setProperty('--drift', `${((i % 9) - 4) * 14}px`);
    host.appendChild(p);
  }
}

export function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] as string);
}
