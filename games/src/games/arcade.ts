import { CVC, FOUR, LONG } from '../content/words';
import type { Rng } from '../engine/rng';
import { numberOptions } from '../engine/rng';
import type { Game, RoundContext } from '../engine/types';
import { createArena, label, roundRect, Sparks } from '../ui/arena';

/**
 * Arcade learning games: real-time play with physics and juicy feedback, where scoring
 * only counts when the child also gets the thinking right. Original designs in familiar
 * casual genres (table hockey, snake, penalty kicks).
 */

interface Fact {
  text: string;
  answer: number;
}

/** Number facts that grow with level: small sums, bigger sums, take-aways, then times tables. */
export function mathFact(level: number, rng: Rng): Fact {
  switch (level) {
    case 1: {
      const a = rng.int(1, 5);
      const b = rng.int(1, 5);
      return { text: `${a} + ${b}`, answer: a + b };
    }
    case 2: {
      const a = rng.int(3, 12);
      const b = rng.int(2, 8);
      return { text: `${a} + ${b}`, answer: a + b };
    }
    case 3: {
      const a = rng.int(8, 20);
      const b = rng.int(2, a - 1);
      return rng.chance(0.5) ? { text: `${a} − ${b}`, answer: a - b } : { text: `${a} + ${b}`, answer: a + b };
    }
    default: {
      const a = rng.int(2, 6);
      const b = rng.int(2, 9);
      return { text: `${a} × ${b}`, answer: a * b };
    }
  }
}

const wait = (ms: number) => new Promise<void>((r) => window.setTimeout(r, ms));

/** Headless QA sets this so real-time games finish without a human; never set in the app. */
function qaAutoFinish(finish: () => void): void {
  if ((window as unknown as { __qaSolve?: boolean }).__qaSolve) window.setTimeout(finish, 1800);
}

/* =========================================================================== */
/* Space Hockey: hit the puck into the goal that shows the right answer.        */
/* =========================================================================== */
export const spaceHockey: Game = {
  id: 'space-hockey',
  title: 'Space Hockey',
  world: 'nuzo',
  skill: 'math.facts',
  minAge: 5,
  maxAge: 10,
  maxLevel: 4,
  rounds: 3,
  intro: () => 'Drag your paddle and shoot the puck into the goal with the right answer!',
  async round(ctx: RoundContext) {
    const fact = mathFact(ctx.level, ctx.rng);
    const options = numberOptions(ctx.rng, fact.answer, 3, ctx.level >= 4 ? 6 : 3, 0);
    ctx.prompt(`${fact.text} = ?`);
    ctx.play.classList.add('arena-field');
    const A = createArena(ctx.play);
    const { w, h } = A;
    const g = A.ctx;
    const sparks = new Sparks();

    const pr = Math.max(26, w * 0.075);
    const r = Math.max(14, w * 0.04);
    const gw = Math.min(w * 0.27, 120);
    const goals = options.map((v, i) => ({ v, x: (w * (i * 2 + 1)) / 6, open: true, flash: 0 }));
    const puck = { x: w / 2, y: h * 0.68, vx: 0, vy: 0 };
    const paddle = { x: w / 2, y: h * 0.86, vx: 0, vy: 0 };
    const blocker = ctx.level >= 3 ? { x: w / 2, y: h * 0.26, dir: 1, speed: 60 + ctx.level * 25, r: 22 } : null;
    let shake = 0;
    let done = false;
    let resolveDone: () => void = () => undefined;
    const finished = new Promise<void>((res) => (resolveDone = res));
    qaAutoFinish(() => {
      if (done) return;
      done = true;
      ctx.correct('GOAL!');
      resolveDone();
    });

    const resetPuck = () => {
      puck.x = w / 2;
      puck.y = h * 0.66;
      puck.vx = 0;
      puck.vy = 0;
    };

    const collide = (cx: number, cy: number, cr: number, cvx: number, cvy: number, bounce: number) => {
      const dx = puck.x - cx;
      const dy = puck.y - cy;
      const dist = Math.hypot(dx, dy) || 0.001;
      if (dist >= cr + r) return false;
      const nx = dx / dist;
      const ny = dy / dist;
      puck.x = cx + nx * (cr + r);
      puck.y = cy + ny * (cr + r);
      const rvx = puck.vx - cvx;
      const rvy = puck.vy - cvy;
      const dot = rvx * nx + rvy * ny;
      if (dot < 0) {
        puck.vx = cvx + (rvx - (1 + bounce) * dot * nx);
        puck.vy = cvy + (rvy - (1 + bounce) * dot * ny);
      }
      return true;
    };

    A.run((dt) => {
      // Player paddle follows the finger in the bottom half.
      const tx = A.pointer.down ? A.pointer.x : paddle.x;
      const ty = A.pointer.down ? Math.max(h * 0.52 + pr, A.pointer.y - pr * 0.6) : paddle.y;
      const nx = Math.min(w - pr, Math.max(pr, paddle.x + (tx - paddle.x) * Math.min(1, dt * 22)));
      const ny = Math.min(h - pr, Math.max(h * 0.52 + pr, paddle.y + (ty - paddle.y) * Math.min(1, dt * 22)));
      paddle.vx = (nx - paddle.x) / dt;
      paddle.vy = (ny - paddle.y) / dt;
      paddle.x = nx;
      paddle.y = ny;

      if (blocker) {
        blocker.x += blocker.dir * blocker.speed * dt;
        if (blocker.x < blocker.r + 10 || blocker.x > w - blocker.r - 10) blocker.dir *= -1;
      }

      if (!done) {
        puck.x += puck.vx * dt;
        puck.y += puck.vy * dt;
        const damp = Math.pow(0.55, dt);
        puck.vx *= damp;
        puck.vy *= damp;
        const speed = Math.hypot(puck.vx, puck.vy);
        // The table tilts gently towards the player so a stopped puck always comes back.
        if (speed < 140) puck.vy += 180 * dt;
        if (speed > 1500) {
          puck.vx *= 1500 / speed;
          puck.vy *= 1500 / speed;
        }
        if (puck.x < r) { puck.x = r; puck.vx = Math.abs(puck.vx) * 0.9; }
        if (puck.x > w - r) { puck.x = w - r; puck.vx = -Math.abs(puck.vx) * 0.9; }
        if (puck.y > h - r) { puck.y = h - r; puck.vy = -Math.abs(puck.vy) * 0.8; }

        if (collide(paddle.x, paddle.y, pr, paddle.vx, paddle.vy, 0.9)) {
          ctx.sfx('tap');
          sparks.burst(puck.x, puck.y, '#9fd4ff', 8, 160);
        }
        if (blocker && collide(blocker.x, blocker.y, blocker.r, blocker.dir * blocker.speed, 0, 0.9)) ctx.sfx('tick');

        if (puck.y < r + 12) {
          const hit = goals.find((gl) => gl.open && Math.abs(puck.x - gl.x) < gw / 2 - r * 0.3);
          if (hit) {
            hit.flash = 1;
            if (hit.v === fact.answer) {
              done = true;
              shake = 10;
              sparks.burst(hit.x, 20, '#ffc145', 30, 320);
              ctx.correct('GOAL!');
              window.setTimeout(resolveDone, 1100);
            } else {
              hit.open = false;
              shake = 5;
              sparks.burst(hit.x, 20, '#ff7a59', 14, 200);
              ctx.wrong(null, `That goal is ${hit.v}. Try another one!`);
              resetPuck();
            }
          } else {
            puck.y = r + 12;
            puck.vy = Math.abs(puck.vy) * 0.85;
          }
        }
      }
      sparks.update(dt);
      shake *= Math.pow(0.02, dt);

      // ---- draw ----
      g.save();
      g.clearRect(0, 0, w, h);
      if (shake > 0.3) g.translate((Math.random() - 0.5) * shake, (Math.random() - 0.5) * shake);
      // rink
      roundRect(g, 4, 4, w - 8, h - 8, 26);
      const bg = g.createLinearGradient(0, 0, 0, h);
      bg.addColorStop(0, '#16205c');
      bg.addColorStop(1, '#0e1440');
      g.fillStyle = bg;
      g.fill();
      g.strokeStyle = 'rgba(159,212,255,.35)';
      g.lineWidth = 3;
      g.stroke();
      g.setLineDash([10, 10]);
      g.beginPath();
      g.moveTo(16, h * 0.52);
      g.lineTo(w - 16, h * 0.52);
      g.stroke();
      g.setLineDash([]);
      g.beginPath();
      g.arc(w / 2, h * 0.52, Math.min(w, h) * 0.12, 0, Math.PI * 2);
      g.stroke();
      // goals
      for (const gl of goals) {
        const glow = gl.open ? (gl.v === fact.answer && done ? '#ffc145' : '#4da8ff') : 'rgba(255,255,255,.18)';
        g.shadowColor = glow;
        g.shadowBlur = gl.open ? 16 + gl.flash * 20 : 0;
        roundRect(g, gl.x - gw / 2, 2, gw, 16, 8);
        g.fillStyle = glow;
        g.fill();
        g.shadowBlur = 0;
        roundRect(g, gl.x - 30, 24, 60, 40, 14);
        g.fillStyle = gl.open ? 'rgba(255,255,255,.95)' : 'rgba(255,255,255,.2)';
        g.fill();
        label(g, String(gl.v), gl.x, 45, 26, gl.open ? '#1e2266' : 'rgba(255,255,255,.5)');
        gl.flash = Math.max(0, gl.flash - dt * 2);
      }
      // blocker
      if (blocker) {
        g.font = `${blocker.r * 2}px sans-serif`;
        g.textAlign = 'center';
        g.textBaseline = 'middle';
        g.fillText('👾', blocker.x, blocker.y);
      }
      // puck
      g.shadowColor = '#ffffff';
      g.shadowBlur = 14;
      g.fillStyle = '#ffffff';
      g.beginPath();
      g.arc(puck.x, puck.y, r, 0, Math.PI * 2);
      g.fill();
      g.shadowBlur = 0;
      g.fillStyle = '#c9d6ff';
      g.beginPath();
      g.arc(puck.x, puck.y, r * 0.55, 0, Math.PI * 2);
      g.fill();
      // paddle
      const pg = g.createRadialGradient(paddle.x - pr * 0.3, paddle.y - pr * 0.3, pr * 0.2, paddle.x, paddle.y, pr);
      pg.addColorStop(0, '#ffe08a');
      pg.addColorStop(1, '#ffc145');
      g.fillStyle = pg;
      g.shadowColor = '#ffc145';
      g.shadowBlur = 20;
      g.beginPath();
      g.arc(paddle.x, paddle.y, pr, 0, Math.PI * 2);
      g.fill();
      g.shadowBlur = 0;
      g.fillStyle = '#e8a20f';
      g.beginPath();
      g.arc(paddle.x, paddle.y, pr * 0.42, 0, Math.PI * 2);
      g.fill();
      sparks.draw(g);
      g.restore();
    });

    await finished;
    A.stop();
    await ctx.explain(`${fact.text} = ${fact.answer}!`);
  },
};

/* =========================================================================== */
/* Snake: steer a friendly snake to eat numbers in order, or letters to spell.  */
/* =========================================================================== */
interface Token {
  label: string;
  x: number;
  y: number;
  eaten: boolean;
  wobble: number;
  cooldown: number;
  decoy: boolean;
}

interface SnakeSpec {
  prompt: string;
  sequence: string[];
  decoys: string[];
  /** Updates the bubble as the child progresses. */
  progress?: (eaten: number) => string;
  hint: (expected: string) => string;
  explain: string;
}

async function playSnake(ctx: RoundContext, spec: SnakeSpec, bodyColor: string, headColor: string): Promise<void> {
  ctx.prompt(spec.prompt);
  ctx.play.classList.add('arena-field');
  const A = createArena(ctx.play);
  const { w, h } = A;
  const g = A.ctx;
  const sparks = new Sparks();
  const tr = Math.max(22, Math.min(w, h) * 0.065);

  const head = { x: w / 2, y: h * 0.8, a: -Math.PI / 2 };
  const speed = 95 + ctx.level * 14;
  const trail: { x: number; y: number }[] = [];
  let segments = 5;
  let next = 0;
  let wrongs = 0;
  let done = false;
  let resolveDone: () => void = () => undefined;
  const finished = new Promise<void>((res) => (resolveDone = res));
  qaAutoFinish(() => {
    if (done) return;
    done = true;
    ctx.correct();
    resolveDone();
  });

  // Place tokens apart from each other and away from the start.
  const tokens: Token[] = [];
  const labels = [...spec.sequence.map((l) => ({ l, decoy: false })), ...spec.decoys.map((l) => ({ l, decoy: true }))];
  for (const { l, decoy } of labels) {
    let tries = 0;
    let x = 0;
    let y = 0;
    do {
      x = tr + 10 + ctx.rng.next() * (w - 2 * tr - 20);
      y = tr + 10 + ctx.rng.next() * (h * 0.72 - 2 * tr);
      tries++;
    } while (tries < 200 && tokens.some((t) => Math.hypot(t.x - x, t.y - y) < tr * 2.6));
    tokens.push({ label: l, x, y, eaten: false, wobble: 0, cooldown: 0, decoy });
  }

  A.run((dt) => {
    if (!done) {
      if (A.pointer.down) {
        const want = Math.atan2(A.pointer.y - head.y, A.pointer.x - head.x);
        let diff = want - head.a;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        const turn = 5 * dt;
        head.a += Math.max(-turn, Math.min(turn, diff));
      }
      head.x += Math.cos(head.a) * speed * dt;
      head.y += Math.sin(head.a) * speed * dt;
      // Walls turn the snake around instead of ending the game.
      if (head.x < 14 || head.x > w - 14) {
        head.a = Math.PI - head.a;
        head.x = Math.min(w - 14, Math.max(14, head.x));
      }
      if (head.y < 14 || head.y > h - 14) {
        head.a = -head.a;
        head.y = Math.min(h - 14, Math.max(14, head.y));
      }
      trail.unshift({ x: head.x, y: head.y });
      if (trail.length > 600) trail.length = 600;

      for (const t of tokens) {
        t.cooldown = Math.max(0, t.cooldown - dt);
        t.wobble = Math.max(0, t.wobble - dt * 3);
        if (t.eaten || Math.hypot(t.x - head.x, t.y - head.y) > tr + 12) continue;
        if (!t.decoy && t.label === spec.sequence[next]) {
          t.eaten = true;
          next++;
          segments += 3;
          ctx.sfx('pop');
          sparks.burst(t.x, t.y, '#ffc145', 16, 220);
          if (spec.progress) ctx.prompt(spec.progress(next));
          if (next === spec.sequence.length) {
            done = true;
            ctx.correct();
            window.setTimeout(resolveDone, 900);
          }
        } else if (t.cooldown === 0) {
          t.cooldown = 1.2;
          t.wobble = 1;
          wrongs++;
          head.a += Math.PI * 0.85;
          ctx.wrong(null, spec.hint(spec.sequence[next]));
        }
      }
    }
    sparks.update(dt);

    // ---- draw ----
    g.clearRect(0, 0, w, h);
    roundRect(g, 4, 4, w - 8, h - 8, 26);
    g.fillStyle = 'rgba(255,255,255,.07)';
    g.fill();
    g.fillStyle = 'rgba(255,255,255,.08)';
    for (let gx = 24; gx < w; gx += 32) for (let gy = 24; gy < h; gy += 32) g.fillRect(gx, gy, 2, 2);

    const hintNext = wrongs >= 2 ? spec.sequence[next] : null;
    for (const t of tokens) {
      if (t.eaten) continue;
      const wob = Math.sin(performance.now() / 40) * 6 * t.wobble;
      const pulse = hintNext === t.label && !t.decoy ? 1 + Math.sin(performance.now() / 180) * 0.08 : 1;
      g.save();
      g.translate(t.x + wob, t.y);
      g.scale(pulse, pulse);
      g.shadowColor = 'rgba(0,0,0,.25)';
      g.shadowBlur = 8;
      g.shadowOffsetY = 4;
      g.fillStyle = '#fff';
      g.beginPath();
      g.arc(0, 0, tr, 0, Math.PI * 2);
      g.fill();
      g.restore();
      label(g, t.label, t.x + wob, t.y + 1, tr * 0.95, '#1e2266');
    }

    // body: circles sampled along the trail
    const spacing = 9;
    for (let s = segments; s >= 1; s--) {
      const p = trail[Math.min(trail.length - 1, s * Math.round(spacing / Math.max(1, (speed / 60))))];
      if (!p) continue;
      g.fillStyle = s % 2 ? bodyColor : headColor;
      g.beginPath();
      g.arc(p.x, p.y, 13 - Math.min(5, s * 0.08), 0, Math.PI * 2);
      g.fill();
    }
    // head with eyes
    g.fillStyle = headColor;
    g.beginPath();
    g.arc(head.x, head.y, 17, 0, Math.PI * 2);
    g.fill();
    for (const side of [-1, 1]) {
      const ex = head.x + Math.cos(head.a) * 7 + Math.cos(head.a + Math.PI / 2) * 7 * side;
      const ey = head.y + Math.sin(head.a) * 7 + Math.sin(head.a + Math.PI / 2) * 7 * side;
      g.fillStyle = '#fff';
      g.beginPath();
      g.arc(ex, ey, 5, 0, Math.PI * 2);
      g.fill();
      g.fillStyle = '#1e2266';
      g.beginPath();
      g.arc(ex + Math.cos(head.a) * 1.5, ey + Math.sin(head.a) * 1.5, 2.6, 0, Math.PI * 2);
      g.fill();
    }
    sparks.draw(g);
  });

  await finished;
  A.stop();
  await ctx.explain(spec.explain);
}

export const numberSnake: Game = {
  id: 'number-snake',
  title: 'Number Snake',
  world: 'nuzo',
  skill: 'math.sequences',
  minAge: 4,
  maxAge: 10,
  maxLevel: 4,
  rounds: 2,
  intro: () => 'Touch where you want the snake to go. Eat the numbers in order!',
  async round(ctx) {
    let sequence: number[];
    let rule: string;
    switch (ctx.level) {
      case 1:
        sequence = [1, 2, 3, 4, 5];
        rule = 'Eat 1 to 5 in order!';
        break;
      case 2: {
        const start = ctx.rng.pick([1, 2]);
        sequence = start === 1 ? [1, 2, 3, 4, 5, 6, 7] : [2, 4, 6, 8, 10];
        rule = start === 1 ? 'Eat 1 to 7 in order!' : 'Count by 2s: 2, 4, 6...';
        break;
      }
      case 3: {
        const step = ctx.rng.pick([5, 10]);
        sequence = Array.from({ length: 5 }, (_, i) => step * (i + 1));
        rule = `Count by ${step}s: ${step}, ${step * 2}...`;
        break;
      }
      default: {
        const step = ctx.rng.pick([2, 3]);
        const top = step * ctx.rng.int(7, 9);
        sequence = Array.from({ length: 5 }, (_, i) => top - step * i);
        rule = `Count down by ${step}s from ${top}!`;
      }
    }
    const decoyCount = ctx.level === 1 ? 0 : ctx.level === 2 ? 1 : 2;
    const decoys: string[] = [];
    while (decoys.length < decoyCount) {
      const d = sequence[0] + ctx.rng.int(1, Math.max(3, sequence[sequence.length - 1]));
      if (!sequence.includes(d) && !decoys.includes(String(d))) decoys.push(String(d));
    }
    await playSnake(
      ctx,
      {
        prompt: rule,
        sequence: sequence.map(String),
        decoys,
        hint: (expected) => `Find ${expected} first!`,
        explain: `${sequence.join(', ')}. You did it!`,
      },
      '#2fc4a6',
      '#17917d',
    );
  },
};

export const wordSnake: Game = {
  id: 'word-snake',
  title: 'Word Snake',
  world: 'lotli',
  skill: 'literacy.spelling',
  minAge: 5,
  maxAge: 10,
  maxLevel: 3,
  rounds: 2,
  intro: () => 'Steer the snake and eat the letters to spell the word!',
  async round(ctx) {
    const word = ctx.rng.pick(ctx.level === 1 ? CVC : ctx.level === 2 ? FOUR : LONG);
    const letters = word.word.split('');
    const pool = 'abcdefghijklmnoprstuw'.split('').filter((l) => !letters.includes(l));
    const decoys = ctx.rng.shuffle(pool).slice(0, ctx.level === 1 ? 1 : 2);
    const shown = (n: number) =>
      `${word.emoji}  ${letters.map((l, i) => (i < n ? l.toUpperCase() : ctx.level === 1 ? l.toUpperCase() : '_')).join(' ')}`;
    await playSnake(
      ctx,
      {
        prompt: ctx.level === 1 ? `Spell ${word.word.toUpperCase()}: ${word.emoji}` : shown(0),
        sequence: letters,
        decoys,
        progress: (n) => shown(n),
        hint: (expected) => `Look for ${expected.toUpperCase()}!`,
        explain: `${letters.map((l) => l.toUpperCase()).join(', ')} spells ${word.word}!`,
      },
      '#ff9ec8',
      '#f06a9f',
    );
  },
};

/* =========================================================================== */
/* Penalty Kick: swipe to shoot into the corner with the right answer.         */
/* =========================================================================== */
export const penaltyKick: Game = {
  id: 'penalty-kick',
  title: 'Penalty Kick',
  world: 'embrix',
  skill: 'math.facts',
  minAge: 5,
  maxAge: 10,
  maxLevel: 4,
  rounds: 3,
  intro: () => 'Swipe up from the ball to shoot. Aim for the right answer!',
  async round(ctx) {
    const fact = mathFact(ctx.level, ctx.rng);
    const options = numberOptions(ctx.rng, fact.answer, 3, ctx.level >= 4 ? 6 : 3, 0);
    ctx.prompt(`${fact.text} = ?`);
    ctx.play.classList.add('arena-field');
    const A = createArena(ctx.play);
    const { w, h } = A;
    const g = A.ctx;
    const sparks = new Sparks();

    const goal = { x: w * 0.08, y: h * 0.1, w: w * 0.84, h: h * 0.24 };
    const zones = options.map((v, i) => ({ v, x: goal.x + (goal.w * (i * 2 + 1)) / 6, y: goal.y + goal.h * 0.55, dead: false }));
    const keeper = { x: w / 2, w: goal.w * 0.2, dir: 1, speed: 40 + ctx.level * 35 };
    const ballHome = { x: w / 2, y: h * 0.82 };
    const ball = { x: ballHome.x, y: ballHome.y, scale: 1 };
    let shot: { fromX: number; fromY: number; toX: number; toY: number; t: number } | null = null;
    let swipeStart: { x: number; y: number } | null = null;
    let netShake = 0;
    let done = false;
    let resolveDone: () => void = () => undefined;
    const finished = new Promise<void>((res) => (resolveDone = res));
    qaAutoFinish(() => {
      if (done) return;
      done = true;
      ctx.correct('GOAL!');
      resolveDone();
    });

    A.onDown((p) => {
      if (shot || done) return;
      if (Math.hypot(p.x - ball.x, p.y - ball.y) < 90) swipeStart = { x: p.x, y: p.y };
    });
    A.onUp((p) => {
      if (!swipeStart || shot || done) return;
      const dx = p.x - swipeStart.x;
      const dy = p.y - swipeStart.y;
      swipeStart = null;
      if (dy > -30) return;
      const toY = goal.y + goal.h * 0.55;
      const toX = ball.x + (dx / -dy) * (ball.y - toY);
      shot = { fromX: ball.x, fromY: ball.y, toX, toY, t: 0 };
      ctx.sfx('whoosh');
    });

    const resetBall = () => {
      ball.x = ballHome.x;
      ball.y = ballHome.y;
      ball.scale = 1;
      shot = null;
    };

    A.run((dt) => {
      // Keeper patrols; at higher levels it also reacts to the shot.
      if (shot && ctx.level >= 3) {
        const toward = Math.sign(shot.toX - keeper.x);
        keeper.x += toward * keeper.speed * 0.9 * dt;
      } else {
        keeper.x += keeper.dir * keeper.speed * dt;
        if (keeper.x < goal.x + keeper.w / 2 || keeper.x > goal.x + goal.w - keeper.w / 2) keeper.dir *= -1;
      }
      keeper.x = Math.min(goal.x + goal.w - keeper.w / 2, Math.max(goal.x + keeper.w / 2, keeper.x));

      if (shot && !done) {
        shot.t = Math.min(1, shot.t + dt / 0.55);
        const e = 1 - Math.pow(1 - shot.t, 2);
        ball.x = shot.fromX + (shot.toX - shot.fromX) * e;
        ball.y = shot.fromY + (shot.toY - shot.fromY) * e - Math.sin(e * Math.PI) * 40;
        ball.scale = 1 - 0.45 * e;
        if (shot.t >= 1) {
          const x = shot.toX;
          if (x < goal.x || x > goal.x + goal.w) {
            ctx.sfx('tick');
            ctx.say('Wide! Try again.');
            window.setTimeout(resetBall, 500);
            shot.t = 2;
          } else if (Math.abs(x - keeper.x) < keeper.w / 2 + 8) {
            sparks.burst(x, shot.toY, '#ffffff', 12, 200);
            ctx.sfx('tap');
            ctx.say('Saved! Try again.');
            window.setTimeout(resetBall, 600);
            shot.t = 2;
          } else {
            const zone = zones.reduce((best, z) => (Math.abs(z.x - x) < Math.abs(best.x - x) ? z : best));
            netShake = 1;
            if (zone.v === fact.answer) {
              done = true;
              sparks.burst(x, shot.toY, '#ffc145', 34, 320);
              ctx.correct('GOAL!');
              window.setTimeout(resolveDone, 1200);
            } else {
              zone.dead = true;
              sparks.burst(x, shot.toY, '#ff7a59', 14, 220);
              ctx.wrong(null, `That corner is ${zone.v}. Try another!`);
              window.setTimeout(resetBall, 700);
              shot.t = 2;
            }
          }
        }
      }
      sparks.update(dt);
      netShake = Math.max(0, netShake - dt * 2);

      // ---- draw ----
      g.clearRect(0, 0, w, h);
      // pitch
      for (let i = 0; i < 8; i++) {
        g.fillStyle = i % 2 ? '#2f9e5b' : '#36ad65';
        g.fillRect(0, (h / 8) * i, w, h / 8 + 1);
      }
      g.strokeStyle = 'rgba(255,255,255,.7)';
      g.lineWidth = 3;
      g.strokeRect(w * 0.18, goal.y + goal.h, w * 0.64, h * 0.22);
      g.beginPath();
      g.arc(ballHome.x, ballHome.y + 4, 4, 0, Math.PI * 2);
      g.fillStyle = '#fff';
      g.fill();
      // net
      g.save();
      g.translate(0, Math.sin(performance.now() / 30) * 3 * netShake);
      g.fillStyle = 'rgba(255,255,255,.12)';
      g.fillRect(goal.x, goal.y, goal.w, goal.h);
      g.strokeStyle = 'rgba(255,255,255,.35)';
      g.lineWidth = 1;
      for (let x = goal.x; x <= goal.x + goal.w; x += 14) {
        g.beginPath();
        g.moveTo(x, goal.y);
        g.lineTo(x, goal.y + goal.h);
        g.stroke();
      }
      for (let y = goal.y; y <= goal.y + goal.h; y += 14) {
        g.beginPath();
        g.moveTo(goal.x, y);
        g.lineTo(goal.x + goal.w, y);
        g.stroke();
      }
      g.restore();
      // targets
      for (const z of zones) {
        g.fillStyle = z.dead ? 'rgba(255,255,255,.25)' : '#ffffff';
        g.beginPath();
        g.arc(z.x, z.y, 26, 0, Math.PI * 2);
        g.fill();
        g.strokeStyle = z.dead ? 'transparent' : '#ff7a59';
        g.lineWidth = 4;
        g.stroke();
        label(g, String(z.v), z.x, z.y + 1, 24, z.dead ? 'rgba(30,34,102,.4)' : '#1e2266');
      }
      // posts
      g.strokeStyle = '#ffffff';
      g.lineWidth = 7;
      g.lineCap = 'round';
      g.beginPath();
      g.moveTo(goal.x, goal.y + goal.h);
      g.lineTo(goal.x, goal.y);
      g.lineTo(goal.x + goal.w, goal.y);
      g.lineTo(goal.x + goal.w, goal.y + goal.h);
      g.stroke();
      // keeper (a friendly robot)
      const ky = goal.y + goal.h - 10;
      roundRect(g, keeper.x - keeper.w / 2, ky - goal.h * 0.62, keeper.w, goal.h * 0.62, 12);
      g.fillStyle = '#4da8ff';
      g.fill();
      g.fillStyle = '#fff';
      g.beginPath();
      g.arc(keeper.x - 9, ky - goal.h * 0.45, 5, 0, Math.PI * 2);
      g.arc(keeper.x + 9, ky - goal.h * 0.45, 5, 0, Math.PI * 2);
      g.fill();
      // ball
      const br = 22 * ball.scale;
      g.shadowColor = 'rgba(0,0,0,.3)';
      g.shadowBlur = 10;
      g.shadowOffsetY = 6;
      g.fillStyle = '#ffffff';
      g.beginPath();
      g.arc(ball.x, ball.y, br, 0, Math.PI * 2);
      g.fill();
      g.shadowBlur = 0;
      g.shadowOffsetY = 0;
      g.fillStyle = '#1e2266';
      g.beginPath();
      for (let i = 0; i < 5; i++) {
        const a = (i / 5) * Math.PI * 2 + performance.now() / (shot ? 80 : 900);
        g.moveTo(ball.x + Math.cos(a) * br * 0.55, ball.y + Math.sin(a) * br * 0.55);
        g.arc(ball.x + Math.cos(a) * br * 0.55, ball.y + Math.sin(a) * br * 0.55, br * 0.18, 0, Math.PI * 2);
      }
      g.fill();
      // swipe guide
      if (swipeStart && A.pointer.down) {
        g.strokeStyle = 'rgba(255,193,69,.9)';
        g.lineWidth = 5;
        g.setLineDash([8, 8]);
        g.beginPath();
        g.moveTo(ball.x, ball.y);
        g.lineTo(ball.x + (A.pointer.x - swipeStart.x) * 1.6, ball.y + (A.pointer.y - swipeStart.y) * 1.6);
        g.stroke();
        g.setLineDash([]);
      } else if (!shot && !done) {
        label(g, '↑ swipe', ball.x, ball.y + 46, 16, 'rgba(255,255,255,.85)');
      }
      sparks.draw(g);
    });

    await finished;
    A.stop();
    await wait(100);
    await ctx.explain(`${fact.text} = ${fact.answer}. What a shot!`);
  },
};

export const ARCADE_GAMES: Game[] = [spaceHockey, numberSnake, wordSnake, penaltyKick];
