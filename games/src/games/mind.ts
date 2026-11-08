import type { Game } from '../engine/types';
import { button, el, replay } from '../ui/dom';

/* ------------------------------------------------------------------------- */
/* 12. Dragon Breath: three calm breaths. Self-regulation before play resumes. */
/* ------------------------------------------------------------------------- */
export const dragonBreath: Game = {
  id: 'dragon-breath',
  title: 'Dragon Breath',
  world: 'embrix',
  skill: 'wellbeing.calm',
  minAge: 4,
  maxAge: 10,
  maxLevel: 1,
  rounds: 3,
  antiGuess: false,
  intro: () => "Let's take three big dragon breaths with Embrix.",
  async round(ctx) {
    ctx.play.classList.add('breath-field');
    const orb = el('div', 'breath-orb', '<div class="breath-core"></div><div class="breath-count"></div>');
    const hold = button('breath-btn', 'Hold to breathe in', () => undefined);
    ctx.play.append(orb, hold);
    const count = orb.querySelector('.breath-count') as HTMLElement;
    const inhaleMs = 3000;
    const exhaleMs = 3500;

    ctx.prompt('Press and hold, and breathe in slowly.');
    await new Promise<void>((resolve) => {
      let start = 0;
      let timer = 0;
      let done = false;
      const tickCount = () => {
        const s = Math.min(3, Math.floor((performance.now() - start) / 1000) + 1);
        count.textContent = String(s);
      };
      const down = (e: Event) => {
        e.preventDefault();
        if (done) return;
        start = performance.now();
        orb.classList.remove('out');
        orb.classList.add('in');
        hold.classList.add('holding');
        tickCount();
        timer = window.setInterval(() => {
          tickCount();
          if (performance.now() - start >= inhaleMs) {
            window.clearInterval(timer);
            done = true;
            hold.classList.remove('holding');
            hold.disabled = true;
            hold.textContent = 'Now breathe out...';
            ctx.prompt('Now let it out slowly, like a dragon.');
            orb.classList.remove('in');
            orb.classList.add('out', 'fire');
            count.textContent = '';
            window.setTimeout(resolve, exhaleMs);
          }
        }, 200);
      };
      const up = () => {
        if (done || !start) return;
        window.clearInterval(timer);
        orb.classList.remove('in');
        hold.classList.remove('holding');
        count.textContent = '';
        start = 0;
        ctx.say('Keep holding a little longer. Try again.');
      };
      hold.addEventListener('pointerdown', down);
      hold.addEventListener('pointerup', up);
      hold.addEventListener('pointerleave', up);
      hold.addEventListener('pointercancel', up);
    });
    ctx.correct(ctx.index === ctx.total - 1 ? 'You feel calm and strong!' : 'Lovely. One more.');
  },
};

/* ------------------------------------------------------------------------- */
/* 13. Power Moves: real movement. Counts jumps with the motion sensor.         */
/* ------------------------------------------------------------------------- */
const MOVES = [
  { name: 'jumps', verb: 'Jump', emoji: '🦘', say: 'Hold the phone and jump' },
  { name: 'squats', verb: 'Squat', emoji: '🏋️', say: 'Hold the phone and squat down and up' },
  { name: 'arm circles', verb: 'Arm circle', emoji: '🙆', say: 'Hold the phone and make big arm circles' },
];

export const powerMoves: Game = {
  id: 'power-moves',
  title: 'Power Moves',
  world: 'embrix',
  skill: 'wellbeing.movement',
  minAge: 4,
  maxAge: 10,
  maxLevel: 3,
  rounds: 2,
  antiGuess: false,
  intro: () => 'Embrix wants to move! Stand up, hold the phone, and copy the move.',
  async round(ctx) {
    const move = ctx.index === 0 ? MOVES[0] : ctx.rng.pick(MOVES.slice(1));
    const target = ctx.level === 1 ? 5 : ctx.level === 2 ? 8 : 10;
    ctx.play.classList.add('move-field');
    const ring = el('div', 'move-ring', `<div class="move-emoji">${move.emoji}</div><div class="move-num">0</div><div class="move-of">of ${target}</div>`);
    const tapBtn = button('move-tap', `Tap for each ${move.verb.toLowerCase()}`, () => bump());
    ctx.play.append(ring, tapBtn);
    const num = ring.querySelector('.move-num') as HTMLElement;
    ctx.prompt(`${target} ${move.name}! Go!`, `${move.say}, ${target} times. Go!`);

    let n = 0;
    let lastPeak = 0;
    let resolveDone: () => void = () => undefined;
    const doneP = new Promise<void>((r) => (resolveDone = r));
    function bump() {
      if (n >= target) return;
      n += 1;
      num.textContent = String(n);
      ring.style.setProperty('--p', String(n / target));
      replay(ring, 'pulse');
      ctx.sfx('tick');
      ctx.say(String(n));
      if (n >= target) resolveDone();
    }
    const onMotion = (e: DeviceMotionEvent) => {
      const a = e.accelerationIncludingGravity;
      if (!a || a.x === null || a.y === null || a.z === null) return;
      const mag = Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
      const now = performance.now();
      if (Math.abs(mag - 9.81) > 7 && now - lastPeak > 450) {
        lastPeak = now;
        bump();
      }
    };
    window.addEventListener('devicemotion', onMotion);
    await doneP;
    window.removeEventListener('devicemotion', onMotion);
    ctx.correct(`${target} ${move.name}! Super strong!`);
    await ctx.explain('Moving your body wakes up your brain.');
  },
};

/* -------------------------------------------------------- */
/* 14. Dragon Memory: find the matching pairs.                 */
/* -------------------------------------------------------- */
const CARD_FACES = ['🐉', '🔥', '⛰️', '🌋', '💎', '🛡️', '👑', '🗝️', '🌟', '🍄'];

export const dragonMemory: Game = {
  id: 'dragon-memory',
  title: 'Dragon Memory',
  world: 'embrix',
  skill: 'cognition.memory',
  minAge: 4,
  maxAge: 10,
  maxLevel: 3,
  rounds: 2,
  antiGuess: false,
  intro: () => 'Find the matching pairs. Remember where each one is!',
  async round(ctx) {
    const pairs = ctx.level === 1 ? 3 : ctx.level === 2 ? 4 : 6;
    const faces = ctx.rng.shuffle(CARD_FACES).slice(0, pairs);
    const deck = ctx.rng.shuffle([...faces, ...faces]);
    ctx.play.classList.add('memory-field');
    const grid = el('div', 'memory-grid');
    grid.style.setProperty('--cols', String(pairs <= 3 ? 3 : 4));
    ctx.play.appendChild(grid);
    ctx.prompt('Tap two cards to find a pair.');

    let open: { face: string; card: HTMLButtonElement }[] = [];
    let found = 0;
    let busy = false;
    await new Promise<void>((resolve) => {
      deck.forEach((face) => {
        const card = button('mem-card', `<span class="mem-back">🐾</span><span class="mem-face">${face}</span>`, () => {
          if (busy || card.classList.contains('flipped')) return;
          card.classList.add('flipped');
          ctx.sfx('tap');
          open.push({ face, card });
          if (open.length < 2) return;
          const [a, b] = open;
          open = [];
          if (a.face === b.face) {
            found += 1;
            a.card.classList.add('matched');
            b.card.classList.add('matched');
            ctx.sfx('pop');
            if (found === pairs) resolve();
          } else {
            busy = true;
            window.setTimeout(() => {
              a.card.classList.remove('flipped');
              b.card.classList.remove('flipped');
              busy = false;
            }, 900);
          }
        });
        grid.appendChild(card);
      });
    });
    ctx.correct('You remembered them all!');
    await ctx.wait(600);
  },
};

export const MIND_GAMES: Game[] = [dragonBreath, powerMoves, dragonMemory];
