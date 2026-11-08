import {
  CATEGORIES,
  CATEGORY_CLASHES,
  CVC,
  FIRST_SOUND,
  FOUR,
  LONG,
  RHYMES,
  SIGHT_WORDS_1,
  SIGHT_WORDS_2,
  type Category,
  type Word,
} from '../content/words';
import type { Game } from '../engine/types';
import { button, el, replay } from '../ui/dom';

const ALPHABET = 'abcdefghijklmnopqrstuvwxyz'.split('');

function picture(word: Word, caption: string | null = word.word): HTMLElement {
  return el('div', 'picture-card', `<div class="picture-emoji">${word.emoji}</div>${caption !== null ? `<div class="picture-word">${caption}</div>` : ''}`);
}

/* ---------------------------------------------------------- */
/* 7. Bubble Letters: hear the first sound, find its letter.     */
/* ---------------------------------------------------------- */
export const bubbleLetters: Game = {
  id: 'bubble-letters',
  title: 'Bubble Letters',
  world: 'lotli',
  skill: 'literacy.phonics',
  minAge: 4,
  maxAge: 7,
  maxLevel: 2,
  rounds: 3,
  intro: () => 'Listen to the word. Which letter does it start with?',
  async round(ctx) {
    const word = ctx.rng.pick(FIRST_SOUND);
    const first = word.word[0];
    ctx.play.classList.add('word-field');
    const caption = ctx.level === 1 ? null : `_${word.word.slice(1)}`;
    ctx.play.appendChild(picture(word, caption));
    ctx.prompt(`What does ${word.word} start with?`, `${word.word}. What letter does ${word.word} start with?`);

    // Never offer both c and k, they share a sound.
    const pool = ALPHABET.filter((l) => l !== first && !(first === 'c' && l === 'k') && !(first === 'k' && l === 'c'));
    const letters = ctx.rng.shuffle([first, ...ctx.rng.shuffle(pool).slice(0, ctx.level === 1 ? 2 : 3)]);
    await ctx.choose(
      letters.map((l) => ({ value: l, label: l, kind: 'letter' as const })),
      (l) => l === first,
      { layout: 'row' },
    );
    await ctx.explain(`${capital(word.word)} starts with ${first.toUpperCase()}.`);
  },
};

/* --------------------------------------------------- */
/* 8. Word Builder: spell the word, letter by letter.     */
/* --------------------------------------------------- */
export const wordBuilder: Game = {
  id: 'word-builder',
  title: 'Word Builder',
  world: 'lotli',
  skill: 'literacy.spelling',
  minAge: 5,
  maxAge: 10,
  maxLevel: 3,
  rounds: 3,
  intro: () => 'Build the word! Tap the letters in the right order.',
  async round(ctx) {
    const word = ctx.rng.pick(ctx.level === 1 ? CVC : ctx.level === 2 ? FOUR : LONG);
    const letters = word.word.split('');
    ctx.play.classList.add('word-field');
    ctx.play.appendChild(picture(word, null));
    const slots = el('div', 'slots');
    letters.forEach(() => slots.appendChild(el('span', 'slot')));
    ctx.play.appendChild(slots);
    ctx.prompt(`Spell ${word.word}.`, `Spell ${word.word}. ${letters.join(', ')}.`);

    const extra = ctx.level >= 2 ? ctx.rng.shuffle(ALPHABET.filter((l) => !letters.includes(l))).slice(0, 2) : [];
    const pile = ctx.rng.shuffle([...letters, ...extra]);
    const tray = el('div', 'letter-pile');
    ctx.play.appendChild(tray);

    let next = 0;
    await new Promise<void>((resolve) => {
      pile.forEach((letter) => {
        const tile = button('letter-tile', letter, () => {
          if (letter === letters[next]) {
            tile.disabled = true;
            tile.classList.add('used');
            const slot = slots.children[next] as HTMLElement;
            slot.textContent = letter;
            replay(slot, 'filled');
            ctx.sfx('pop');
            next += 1;
            if (next === letters.length) {
              ctx.correct();
              resolve();
            }
          } else {
            ctx.wrong(
              tile,
              next === 0
                ? `Listen: ${word.word}. Which letter comes first?`
                : `Listen: ${word.word}. What comes after ${letters.slice(0, next).join(', ')}?`,
            );
          }
        });
        tray.appendChild(tile);
      });
    });
    await ctx.explain(`${letters.map((l) => l.toUpperCase()).join(', ')} spells ${word.word}!`);
  },
};

/* ------------------------------------------------- */
/* 9. Rhyme Lagoon: hear which words sound alike.       */
/* ------------------------------------------------- */
export const rhymeLagoon: Game = {
  id: 'rhyme-lagoon',
  title: 'Rhyme Lagoon',
  world: 'lotli',
  skill: 'literacy.rhyme',
  minAge: 4,
  maxAge: 8,
  maxLevel: 2,
  rounds: 3,
  intro: () => 'Rhyming words sound the same at the end. Like cat and hat!',
  async round(ctx) {
    const families = RHYMES.filter((f) => f.length >= 2);
    const family = ctx.rng.pick(families);
    const [target, match] = ctx.rng.shuffle(family);
    const others = ctx.rng
      .shuffle(families.filter((f) => f !== family).flat())
      .filter((w) => !w.word.endsWith(target.word.slice(-2)))
      .slice(0, 2);

    ctx.play.classList.add('word-field');
    ctx.play.appendChild(picture(target));
    ctx.prompt(`What rhymes with ${target.word}?`);
    const choices = ctx.rng.shuffle([match, ...others]).map((w) => ({
      value: w.word,
      label: ctx.level === 1 ? `${w.emoji} ${w.word}` : w.word,
      kind: 'word' as const,
      speak: w.word,
    }));
    await ctx.choose(choices, (v) => v === match.word, {
      layout: 'column',
      coach: (v) => `${capital(target.word)}, ${v}. Do they sound the same at the end?`,
    });
    await ctx.explain(`${capital(target.word)} and ${match.word} rhyme!`);
  },
};

/* ------------------------------------------------------------ */
/* 10. Odd One Out: sort by category and say why.                  */
/* ------------------------------------------------------------ */
export const oddOneOut: Game = {
  id: 'odd-one-out',
  title: 'Odd One Out',
  world: 'lotli',
  skill: 'logic.categories',
  minAge: 4,
  maxAge: 10,
  maxLevel: 3,
  rounds: 3,
  intro: () => "Three of these belong together. Which one doesn't?",
  async round(ctx) {
    const pool = CATEGORIES.filter((c) => c.level <= ctx.level && c.items.length >= 3);
    const preferred = pool.filter((c) => c.level === ctx.level);
    const group: Category = ctx.rng.pick(preferred.length ? preferred : pool);
    const clashes = new Set([group.name, ...(CATEGORY_CLASHES[group.name] ?? [])]);
    const groupWords = new Set(group.items.map((w) => w.word));
    const outsiders = CATEGORIES.filter((c) => !clashes.has(c.name) && !(CATEGORY_CLASHES[c.name] ?? []).includes(group.name));
    const oddCat = ctx.rng.pick(outsiders);
    const odd = ctx.rng.pick(oddCat.items.filter((w) => !groupWords.has(w.word)));
    const three = ctx.rng.shuffle(group.items).slice(0, 3);

    ctx.play.classList.add('word-field', 'odd-field');
    ctx.prompt('Which one does not belong?');
    const choices = ctx.rng.shuffle([...three, odd]).map((w) => ({
      value: w.word,
      label: w.emoji,
      kind: 'emoji' as const,
      speak: w.word,
    }));
    await ctx.choose(choices, (v) => v === odd.word, {
      coach: (v) => `${capital(articled(v))} is ${group.noun}. Look for the one that is different.`,
    });
    await ctx.explain(`${capital(articled(odd.word))} is ${oddCat.noun}. The others are ${group.name}.`);
  },
};

/* ------------------------------------------------------- */
/* 11. Word Fishing: read common sight words.                 */
/* ------------------------------------------------------- */
export const wordFishing: Game = {
  id: 'word-fishing',
  title: 'Word Fishing',
  world: 'lotli',
  skill: 'literacy.reading',
  minAge: 5,
  maxAge: 9,
  maxLevel: 2,
  rounds: 3,
  intro: () => 'Listen, then catch the fish with that word!',
  async round(ctx) {
    let target: string;
    let words: string[];
    if (ctx.level === 1) {
      words = ctx.rng.shuffle(SIGHT_WORDS_1).slice(0, 3);
      target = words[0];
    } else {
      const [a, b] = ctx.rng.pick(SIGHT_WORDS_2);
      target = ctx.rng.chance(0.5) ? a : b;
      const extra = ctx.rng.shuffle(SIGHT_WORDS_1.filter((w) => w !== a && w !== b)).slice(0, 1);
      words = [a, b, ...extra];
    }
    ctx.play.classList.add('fish-field');
    const pond = el('div', 'pond');
    ctx.play.appendChild(pond);
    ctx.prompt(`Find the word: ${target}`, `Find the word ${target}.`);

    await new Promise<void>((resolve) => {
      ctx.rng.shuffle(words).forEach((w, i) => {
        const fish = button('fish', `<span class="fish-body">${w}</span><span class="fish-tail"></span>`, () => {
          if (w === target) {
            fish.classList.add('caught');
            fish.disabled = true;
            ctx.correct();
            resolve();
          } else {
            fish.classList.add('swim-away');
            fish.disabled = true;
            ctx.wrong(fish, `That says ${w}. Listen: ${target}.`);
          }
        });
        fish.style.setProperty('--i', String(i));
        fish.style.animationDelay = `${i * -1.3}s`;
        pond.appendChild(fish);
      });
    });
    await ctx.explain(`You read ${target}!`);
  },
};

function capital(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

function articled(word: string): string {
  if (/s$/.test(word) && !/ss$/.test(word) && word !== 'bus') return word;
  return `${/^[aeiou]/.test(word) ? 'an' : 'a'} ${word}`;
}

export const WORD_GAMES: Game[] = [bubbleLetters, wordBuilder, rhymeLagoon, oddOneOut, wordFishing];
