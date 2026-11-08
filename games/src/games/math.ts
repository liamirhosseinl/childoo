import { numberOptions } from '../engine/rng';
import type { Game, RoundContext } from '../engine/types';
import { button, el, plural, replay } from '../ui/dom';

const SPACE_THINGS = [
  { emoji: '⭐', one: 'star', many: 'stars' },
  { emoji: '🚀', one: 'rocket', many: 'rockets' },
  { emoji: '👾', one: 'alien', many: 'aliens' },
  { emoji: '🌙', one: 'moon', many: 'moons' },
  { emoji: '☄️', one: 'comet', many: 'comets' },
];

/* ------------------------------------------------------------------ */
/* 1. Star Counter: counting, then grouping in fives, then arrays (×). */
/* ------------------------------------------------------------------ */
export const starCounter: Game = {
  id: 'star-counter',
  title: 'Star Counter',
  world: 'nuzo',
  skill: 'math.counting',
  minAge: 4,
  maxAge: 9,
  maxLevel: 4,
  rounds: 3,
  intro: (level) =>
    level >= 4
      ? 'Rows and columns! Count smart, not one by one.'
      : 'Count the space things. Tap each one to keep track!',
  async round(ctx: RoundContext) {
    const thing = ctx.rng.pick(SPACE_THINGS);
    let n: number;
    let rows = 0;
    let cols = 0;
    if (ctx.level === 1) n = ctx.rng.int(2, 5);
    else if (ctx.level === 2) n = ctx.rng.int(5, 10);
    else if (ctx.level === 3) n = ctx.rng.int(11, 20);
    else {
      rows = ctx.rng.int(2, 5);
      cols = ctx.rng.int(3, 5);
      n = rows * cols;
    }

    ctx.play.classList.add('count-field');
    const field = el('div', ctx.level >= 3 ? 'count-grid' : 'count-scatter');
    if (rows) field.style.setProperty('--cols', String(cols));
    else if (ctx.level === 3) field.style.setProperty('--cols', '5');
    ctx.play.appendChild(field);

    let tally = 0;
    const positions = scatter(n, ctx.rng.next.bind(ctx.rng));
    for (let i = 0; i < n; i++) {
      const item = button('count-item', `<span class="count-emoji">${thing.emoji}</span><span class="count-badge"></span>`, () => {
        if (item.classList.contains('counted')) return;
        tally += 1;
        item.classList.add('counted');
        (item.querySelector('.count-badge') as HTMLElement).textContent = String(tally);
        ctx.sfx('tick');
        ctx.say(String(tally));
      });
      if (ctx.level <= 2) {
        // A little jitter so it feels like a sky, not a grid; still easy to count.
        item.style.transform = `translate(${positions[i].x}px, ${positions[i].y}px) rotate(${positions[i].r}deg)`;
      }
      field.appendChild(item);
    }

    const question = rows ? `${rows} rows of ${cols}. How many ${thing.many}?` : `How many ${thing.many}?`;
    ctx.prompt(question);
    const options = numberOptions(ctx.rng, n, rows ? 3 : 4, rows ? Math.max(2, cols - 1) : ctx.level >= 3 ? 3 : 2, 1);
    await ctx.choose(
      options.map((v) => ({ value: v, label: String(v) })),
      (v) => v === n,
      { coach: (v) => (v < n ? 'Too few. Count again!' : 'Too many. Count again!') },
    );
    if (rows) await ctx.explain(`${rows} rows of ${cols} makes ${n}. That is ${rows} times ${cols}!`);
    else if (ctx.level === 3) await ctx.explain(`Groups of five help: ${Math.floor(n / 5)} fives and ${n % 5} more make ${n}.`);
    else await ctx.explain(`${n} ${plural(n, thing.one, thing.many)}!`);
  },
};

/** Small random offsets and tilts for a playful but countable layout. */
function scatter(n: number, rnd: () => number): { x: number; y: number; r: number }[] {
  return Array.from({ length: n }, () => ({ x: Math.round((rnd() - 0.5) * 16), y: Math.round((rnd() - 0.5) * 18), r: Math.round((rnd() - 0.5) * 30) }));
}

/* ---------------------------------------------------------------------- */
/* 2. Rocket Fuel: number bonds (what makes 5, 10, 20; tens to 100).        */
/* ---------------------------------------------------------------------- */
export const rocketFuel: Game = {
  id: 'rocket-fuel',
  title: 'Rocket Fuel',
  world: 'nuzo',
  skill: 'math.number-bonds',
  minAge: 5,
  maxAge: 10,
  maxLevel: 4,
  rounds: 3,
  intro: () => 'Nuzo needs a full tank to blast off. How much more fuel do we need?',
  async round(ctx) {
    const target = ctx.level === 1 ? 5 : ctx.level === 2 ? 10 : ctx.level === 3 ? 20 : 100;
    const unit = ctx.level === 4 ? 10 : 1;
    const cells = target / unit;
    const filledCells = ctx.rng.int(1, cells - 1);
    const have = filledCells * unit;
    const need = target - have;

    ctx.play.classList.add('fuel-field');
    const rocket = el('div', 'fuel-rocket', '🚀');
    const tank = el('div', 'fuel-tank');
    tank.style.setProperty('--cols', String(Math.min(cells, 5)));
    const slots: HTMLElement[] = [];
    for (let i = 0; i < cells; i++) {
      const c = el('div', `fuel-cell${i < filledCells ? ' full' : ''}`, unit > 1 ? '<b>10</b>' : '');
      slots.push(c);
      tank.appendChild(c);
    }
    ctx.play.append(rocket, tank);

    ctx.prompt(`The tank holds ${target}. It has ${have}. How many more?`);
    const options = numberOptions(ctx.rng, need / unit, 4, 2, 1).map((v) => v * unit);
    await ctx.choose(
      options.map((v) => ({ value: v, label: String(v) })),
      (v) => v === need,
      { coach: (v) => (have + v > target ? `${have} and ${v} is more than ${target}. Too much!` : `${have} and ${v} is not ${target} yet.`) },
    );
    for (let i = filledCells; i < cells; i++) {
      slots[i].classList.add('full', 'fill-in');
      slots[i].style.animationDelay = `${(i - filledCells) * 70}ms`;
    }
    ctx.sfx('whoosh');
    replay(rocket, 'launch');
    await ctx.explain(`${have} and ${need} make ${target}. Blast off!`);
  },
};

/* ------------------------------------------------------------------------ */
/* 3. Space Scale: equality as balance. Wrong answers tip the scale.          */
/* ------------------------------------------------------------------------ */
export const spaceScale: Game = {
  id: 'space-scale',
  title: 'Space Scale',
  world: 'nuzo',
  skill: 'math.equality',
  minAge: 5,
  maxAge: 10,
  maxLevel: 4,
  rounds: 3,
  intro: () => 'Make the scale balance. Both sides must weigh the same!',
  async round(ctx) {
    let left: number[];
    let right: number[];
    let answer: number;
    switch (ctx.level) {
      case 1: {
        answer = ctx.rng.int(1, 6);
        left = [answer];
        right = [];
        break;
      }
      case 2: {
        const a = ctx.rng.int(1, 6);
        const b = ctx.rng.int(1, 4);
        left = [a, b];
        right = [];
        answer = a + b;
        break;
      }
      case 3: {
        const a = ctx.rng.int(2, 7);
        const b = ctx.rng.int(1, 5);
        const c = ctx.rng.int(1, a + b - 1);
        left = [a, b];
        right = [c];
        answer = a + b - c;
        break;
      }
      default: {
        const a = ctx.rng.int(4, 9);
        const b = ctx.rng.int(3, 9);
        const c = ctx.rng.int(2, a + b - 2);
        left = [a, b];
        right = [c];
        answer = a + b - c;
      }
    }
    const leftSum = left.reduce((s, v) => s + v, 0);
    const rightKnown = right.reduce((s, v) => s + v, 0);

    ctx.play.classList.add('scale-field');
    const scale = el('div', 'scale');
    scale.innerHTML = `
      <div class="scale-beam">
        <div class="scale-pan pan-left">${left.map(stack).join('<i class="plus">+</i>')}</div>
        <div class="scale-pan pan-right">${right.map(stack).join('<i class="plus">+</i>')}${right.length ? '<i class="plus">+</i>' : ''}<div class="mystery">?</div></div>
      </div>
      <div class="scale-post"></div>`;
    ctx.play.appendChild(scale);
    const beam = scale.querySelector('.scale-beam') as HTMLElement;
    const tilt = (deg: number) => beam.style.setProperty('--tilt', `${deg}deg`);
    tilt(-10);

    const eq = `${left.join(' + ')} = ${right.length ? right.join(' + ') + ' + ' : ''}?`;
    ctx.prompt(`What makes it balance?  ${eq}`, 'What number makes both sides the same?');
    const options = numberOptions(ctx.rng, answer, 4, 3, 1);
    await ctx.choose(
      options.map((v) => ({ value: v, label: String(v) })),
      (v) => v === answer,
      {
        coach: (v) => {
          const r = rightKnown + v;
          tilt(r > leftSum ? 10 : -10);
          window.setTimeout(() => tilt(-10), 1200);
          return r > leftSum ? 'Too heavy! That side went down.' : 'Too light! That side went up.';
        },
      },
    );
    (scale.querySelector('.mystery') as HTMLElement).textContent = String(answer);
    scale.querySelector('.mystery')?.classList.add('solved');
    tilt(0);
    await ctx.explain(`${left.join(' + ')} is ${leftSum}, and ${right.length ? `${right.join(' + ')} + ${answer}` : answer} is ${leftSum} too. Balanced!`);
  },
};

function stack(n: number): string {
  const cubes = Array.from({ length: n }, () => '<i class="cube"></i>').join('');
  return `<span class="stack" data-n="${n}"><span class="cubes">${cubes}</span><b>${n}</b></span>`;
}

/* ---------------------------------------------------------------- */
/* 4. Moon Jump: where numbers live on a line (magnitude).            */
/* ---------------------------------------------------------------- */
export const moonJump: Game = {
  id: 'moon-jump',
  title: 'Moon Jump',
  world: 'nuzo',
  skill: 'math.number-line',
  minAge: 6,
  maxAge: 10,
  maxLevel: 3,
  rounds: 3,
  intro: () => 'Help Nuzo jump to the right spot on the number line.',
  async round(ctx) {
    const max = ctx.level === 1 ? 10 : ctx.level === 2 ? 20 : 100;
    const step = ctx.level === 3 ? 5 : 1;
    const labelEvery = ctx.level === 1 ? 5 : 10;
    const labelEveryL3 = 50;
    let target = ctx.rng.int(1, max / step - 1) * step;
    if (target % (ctx.level === 3 ? labelEveryL3 : labelEvery) === 0) target += step;

    ctx.play.classList.add('line-field');
    const line = el('div', 'numline');
    const hopper = el('div', 'hopper', '🚀');
    line.appendChild(hopper);
    const spots = max / step;
    let done = false;
    const result = new Promise<void>((resolve) => {
      for (let i = 0; i <= spots; i++) {
        const value = i * step;
        const pct = (value / max) * 100;
        const labelled = ctx.level === 3 ? value % labelEveryL3 === 0 : value % labelEvery === 0;
        const tick = el('div', `tick${labelled ? ' major' : ''}`, labelled ? `<span>${value}</span>` : '');
        tick.style.left = `${pct}%`;
        line.appendChild(tick);
        const hit = button('tick-hit', '', () => {
          if (done) return;
          hopper.style.left = `${pct}%`;
          replay(hopper, 'hop');
          ctx.sfx('pop');
          if (value === target) {
            done = true;
            tick.classList.add('found');
            tick.innerHTML = `<span>${value}</span>`;
            ctx.correct();
            resolve();
          } else {
            const mark = el('div', 'miss', `<span>${value}</span>`);
            mark.style.left = `${pct}%`;
            line.appendChild(mark);
            window.setTimeout(() => mark.remove(), 1600);
            ctx.wrong(null, value < target ? `That's ${value}. ${target} is further along!` : `That's ${value}. ${target} is further back!`);
          }
        });
        hit.style.left = `${pct}%`;
        hit.style.width = `${100 / spots}%`;
        line.appendChild(hit);
      }
    });
    ctx.play.appendChild(line);
    hopper.style.left = '0%';
    ctx.prompt(`Jump to ${target}!`);
    await result;
    await ctx.wait(400);
    const near = Math.round(target / labelEvery) * labelEvery;
    await ctx.explain(
      ctx.level === 3
        ? `${target} is ${target < 50 ? 'less' : 'more'} than halfway to 100.`
        : `${target} is close to ${near}.`,
    );
  },
};

/* --------------------------------------------------------------------- */
/* 5. Fair Share: sharing equally (the idea behind division and fractions). */
/* --------------------------------------------------------------------- */
export const fairShare: Game = {
  id: 'fair-share',
  title: 'Fair Share',
  world: 'nuzo',
  skill: 'math.sharing',
  minAge: 5,
  maxAge: 10,
  maxLevel: 3,
  rounds: 2,
  intro: () => 'Share the space cookies so every friend gets the same. Tap a plate to give it a cookie.',
  async round(ctx) {
    const plates = ctx.level === 1 ? 2 : ctx.level === 2 ? ctx.rng.int(2, 3) : ctx.rng.int(3, 4);
    const each = ctx.level === 1 ? ctx.rng.int(2, 4) : ctx.level === 2 ? ctx.rng.int(2, 4) : ctx.rng.int(3, 5);
    const total = plates * each;
    const friends = ['👽', '🐱', '🤖', '🐶'];

    ctx.play.classList.add('share-field');
    const jar = el('div', 'jar');
    const plateRow = el('div', 'plates');
    plateRow.style.setProperty('--n', String(plates));
    ctx.play.append(jar, plateRow);

    let inJar = total;
    const counts = Array.from({ length: plates }, () => 0);
    const renderJar = () => {
      jar.innerHTML = `<div class="jar-cookies">${'<i>🍪</i>'.repeat(inJar)}</div><span class="jar-label">${inJar} left</span>`;
    };
    renderJar();

    const plateEls = counts.map((_, p) => {
      const plate = el('div', 'plate');
      plate.innerHTML = `<div class="friend">${friends[p]}</div><button type="button" class="plate-dish" aria-label="plate ${p + 1}"></button><div class="plate-count">0</div>`;
      plateRow.appendChild(plate);
      return plate;
    });

    const render = (p: number) => {
      const dish = plateEls[p].querySelector('.plate-dish') as HTMLElement;
      dish.innerHTML = Array.from({ length: counts[p] }, () => '<i class="cookie">🍪</i>').join('');
      (plateEls[p].querySelector('.plate-count') as HTMLElement).textContent = String(counts[p]);
    };

    ctx.prompt(`Share ${total} cookies between ${plates} friends.`);
    await new Promise<void>((resolve) => {
      let finished = false;
      plateEls.forEach((plate, p) => {
        const dish = plate.querySelector('.plate-dish') as HTMLButtonElement;
        dish.addEventListener('click', (ev) => {
          if (finished) return;
          const target = ev.target as HTMLElement;
          if (target.classList.contains('cookie') && counts[p] > 0) {
            // Tapping a cookie on a plate puts it back in the jar.
            counts[p] -= 1;
            inJar += 1;
            ctx.sfx('tap');
          } else if (inJar > 0) {
            counts[p] += 1;
            inJar -= 1;
            ctx.sfx('pop');
          } else {
            return;
          }
          render(p);
          renderJar();
          if (inJar === 0) {
            if (counts.every((c) => c === each)) {
              finished = true;
              ctx.correct('Fair! Everyone has the same.');
              resolve();
            } else {
              ctx.wrong(plateRow, 'Is that fair? Tap a cookie to take it back.');
            }
          }
        });
      });
    });
    await ctx.explain(`${total} shared by ${plates} is ${each} each.`);
  },
};

/* ------------------------------------------------------------------- */
/* 6. Pattern Path: repeating patterns, then growing number patterns.     */
/* ------------------------------------------------------------------- */
const SHAPES = ['🔴', '🔵', '🟡', '🟢', '🟣'];
const SHAPE_NAMES: Record<string, string> = { '🔴': 'red', '🔵': 'blue', '🟡': 'yellow', '🟢': 'green', '🟣': 'purple' };

export const patternPath: Game = {
  id: 'pattern-path',
  title: 'Pattern Path',
  world: 'nuzo',
  skill: 'logic.patterns',
  minAge: 4,
  maxAge: 10,
  maxLevel: 4,
  rounds: 3,
  intro: (level) => (level <= 2 ? 'Find the pattern. What comes next?' : 'Number patterns! Find the rule.'),
  async round(ctx) {
    ctx.play.classList.add('pattern-field');
    const row = el('div', 'pattern-row');
    ctx.play.appendChild(row);

    if (ctx.level <= 2) {
      const [a, b, c] = ctx.rng.shuffle(SHAPES).slice(0, 3);
      const units = ctx.level === 1 ? [[a, b]] : [[a, a, b], [a, b, b], [a, b, c]];
      const unit = ctx.rng.pick(units);
      const shown = ctx.level === 1 ? 5 : 7;
      const seq = Array.from({ length: shown + 1 }, (_, i) => unit[i % unit.length]);
      const answer = seq[shown];
      row.innerHTML = seq.slice(0, shown).map((s) => `<span class="pat">${s}</span>`).join('') + '<span class="pat mystery">?</span>';
      ctx.prompt('What comes next?');
      const options = ctx.rng.shuffle([...new Set([answer, ...unit, ...SHAPES])].slice(0, 3));
      if (!options.includes(answer)) options[0] = answer;
      await ctx.choose(ctx.rng.shuffle(options).map((v) => ({ value: v, label: v, kind: 'emoji' as const })), (v) => v === answer);
      (row.querySelector('.mystery') as HTMLElement).textContent = answer;
      row.querySelector('.mystery')?.classList.add('solved');
      await ctx.explain(`The pattern goes ${unit.map((u) => SHAPE_NAMES[u]).join(', ')}, again and again.`);
      return;
    }

    let seq: number[];
    let rule: string;
    if (ctx.level === 3) {
      const step = ctx.rng.pick([2, 5, 10]);
      const start = ctx.rng.int(0, 5) * (step === 2 ? 1 : step);
      seq = Array.from({ length: 5 }, (_, i) => start + i * step);
      rule = `It goes up by ${step} each time.`;
    } else {
      const kind = ctx.rng.pick(['add3', 'down', 'double'] as const);
      if (kind === 'add3') {
        const step = ctx.rng.pick([3, 4]);
        const start = ctx.rng.int(1, 6);
        seq = Array.from({ length: 5 }, (_, i) => start + i * step);
        rule = `It goes up by ${step} each time.`;
      } else if (kind === 'down') {
        const step = ctx.rng.pick([2, 3, 5]);
        const start = step * ctx.rng.int(6, 9);
        seq = Array.from({ length: 5 }, (_, i) => start - i * step);
        rule = `It goes down by ${step} each time.`;
      } else {
        const start = ctx.rng.pick([1, 2, 3]);
        seq = Array.from({ length: 5 }, (_, i) => start * 2 ** i);
        rule = 'Each number is double the one before.';
      }
    }
    const answer = seq[4];
    row.innerHTML = seq.slice(0, 4).map((s) => `<span class="pat num">${s}</span>`).join('') + '<span class="pat num mystery">?</span>';
    ctx.prompt('What number comes next?');
    const diff = Math.abs(seq[1] - seq[0]) || 1;
    const options = numberOptions(ctx.rng, answer, 4, Math.max(2, diff), 0);
    await ctx.choose(options.map((v) => ({ value: v, label: String(v) })), (v) => v === answer);
    (row.querySelector('.mystery') as HTMLElement).textContent = String(answer);
    row.querySelector('.mystery')?.classList.add('solved');
    await ctx.explain(rule);
  },
};

export const MATH_GAMES: Game[] = [starCounter, rocketFuel, spaceScale, moonJump, fairShare, patternPath];
