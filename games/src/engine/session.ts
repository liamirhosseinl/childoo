import type { Host } from '../bridge';
import type { Stage } from '../ui/stage';
import { applyEarnings, earn, levelFromXp, normalizeWallet } from './economy';
import { createRng, type Rng } from './rng';
import { defaultLevel, looksLikeGuessing, nextLevel } from './rules';
import type { Choice, ChooseOptions, Game, HostConfig, Mood, RoundContext, RoundRecord, SessionResult, SfxName, World } from './types';
import type { Sound } from './sound';

const PRAISE = ['Yes!', 'You got it!', 'Brilliant!', 'Great thinking!', 'Nailed it!', 'Super!'];
const COACH = ["Let's try another way.", 'Hmm, look again.', 'Almost! Try another one.', 'Good try! Think again.'];
const SLOW_DOWN = "Let's slow down and do one more together.";

export interface SessionDeps {
  stage: Stage;
  sound: Sound;
  host: Host;
}

/** Runs one game from intro to celebration and returns what happened, for the host to store. */
export async function runSession(game: Game, config: HostConfig, deps: SessionDeps): Promise<SessionResult> {
  const { stage, sound, host } = deps;
  const age = config.age ?? 6;
  const seed = config.seed ?? Date.now();
  const rng = createRng(seed ^ hash(game.id));
  const level = Math.max(1, Math.min(game.maxLevel, config.forceLevel ?? config.levels?.[game.id] ?? defaultLevel(age, game.maxLevel)));
  const started = performance.now();
  let lastSpoken = '';

  const speak = (text: string) => {
    lastSpoken = text;
    if (config.voice !== false) host.speak(text);
  };
  stage.onReplayRequest = () => lastSpoken && host.speak(lastSpoken);
  stage.setVoice(config.voice !== false);

  const wallet = normalizeWallet(config.wallet);
  let companion: World | null = config.companion ?? null;
  let pickedNow = false;
  if (!companion) {
    stage.setWorld(game.world, game.title);
    companion = await stage.pickBuddy(speak, () => sound.play('pop'));
    pickedNow = true;
    sound.play('correct');
  }

  stage.setWorld(game.world, game.title);
  const records: RoundRecord[] = [];
  let total = game.rounds;
  let extra = 0;
  stage.setStars(total, 0);

  stage.mascot('talk');
  const intro = game.intro(level);
  stage.prompt(intro);
  speak(intro);
  await wait(readingTime(intro));

  for (let index = 0; index < total; index++) {
    stage.clearRound();
    stage.mascot('thinking');
    const record: RoundRecord = { wrongAt: [], solvedAt: 0 };
    const t0 = performance.now();
    let solved = false;

    const ctx: RoundContext = {
      level,
      index,
      total,
      age,
      rng: roundRng(rng),
      play: stage.play,
      prompt: (text, spoken) => {
        stage.prompt(text);
        speak(spoken ?? text);
      },
      say: speak,
      choose: <T,>(choices: Choice<T>[], isCorrect: (v: T) => boolean, options: ChooseOptions<T> = {}) =>
        new Promise<T>((resolve) => {
          const buttons = stage.renderChoices(choices, options, (choice, el) => {
            sound.play('tap');
            if (isCorrect(choice.value)) {
              el.classList.add('right');
              buttons.forEach((b) => (b.disabled = true));
              ctx.correct();
              window.setTimeout(() => resolve(choice.value), 750);
            } else {
              el.disabled = true;
              el.classList.add('wrong');
              ctx.wrong(el, options.coach?.(choice.value));
            }
          });
        }),
      wrong: (el, line) => {
        if (solved) return;
        record.wrongAt.push(performance.now() - t0);
        sound.play('wrong');
        host.haptic('error');
        if (el) shake(el);
        stage.mascot('oops', 1300);
        const msg = line ?? rng.pick(COACH);
        stage.feedback(msg, 'try');
        speak(msg);
      },
      correct: (line) => {
        if (solved) return;
        solved = true;
        record.solvedAt = performance.now() - t0;
        sound.play('correct');
        host.haptic('success');
        stage.mascot('happy');
        const msg = line ?? rng.pick(PRAISE);
        stage.feedback(msg, 'good');
        speak(msg);
      },
      explain: async (text) => {
        stage.feedback(text, 'info');
        speak(text);
        await wait(readingTime(text));
      },
      mascot: (mood: Mood) => stage.mascot(mood),
      sfx: (name: SfxName) => sound.play(name),
      wait,
    };

    await game.round(ctx);
    if (!solved) ctx.correct(); // Games that finish without a right/wrong moment (breathing, movement).
    records.push(record);
    sound.play('star');
    stage.setStars(total, index + 1);
    await wait(500);

    const lastBaseRound = index === total - 1;
    if (lastBaseRound && extra === 0 && game.antiGuess !== false && looksLikeGuessing(records)) {
      extra = 1;
      total += 1;
      stage.setStars(total, index + 1);
      stage.mascot('talk');
      stage.prompt(SLOW_DOWN);
      speak(SLOW_DOWN);
      await wait(readingTime(SLOW_DOWN));
    }
  }

  stage.clearRound();
  const guessing = game.antiGuess !== false && looksLikeGuessing(records.slice(0, game.rounds));

  const firstTry = records.filter((r) => r.wrongAt.length === 0).length;
  const streakDays = Math.max(1, config.streakDays ?? 1);
  const earnings = earn(records.length, firstTry, streakDays);
  const newWallet = applyEarnings(wallet, earnings.total);
  const levelBefore = levelFromXp(wallet.xp);
  const levelAfter = levelFromXp(newWallet.xp);

  const result: SessionResult = {
    mode: 'challenge',
    gameId: game.id,
    skill: game.skill,
    world: game.world,
    level,
    newLevel: nextLevel(level, game.maxLevel, records, guessing),
    rounds: records.length,
    firstTry,
    wrong: records.reduce((n, r) => n + r.wrongAt.length, 0),
    extraRounds: extra,
    guessing,
    durationMs: Math.round(performance.now() - started),
    coinsEarned: earnings.total,
    companion: pickedNow ? companion : config.companion ?? companion,
    wallet: newWallet,
  };

  sound.play('win');
  const name = config.childName ? `, ${config.childName}` : '';
  const title = `Amazing work${name}!`;
  const levelUp = levelAfter > levelBefore;
  const gameLevelUp = result.newLevel > level ? { title: game.title, level: result.newLevel } : undefined;
  stage.celebrate({
    gameLevelUp,
    title,
    sub: 'Your play time is unlocked. Have fun!',
    world: companion,
    equipped: newWallet.equipped,
    coins: earnings.total,
    streakDays,
    streakBonus: earnings.streakBonus,
    levelUp,
    level: levelAfter,
    onTick: () => sound.play('tick'),
  });
  speak(
    levelUp
      ? `${title} You earned ${earnings.total} coins, and your buddy reached level ${levelAfter}!`
      : `${title} You earned ${earnings.total} coins.`,
  );
  await wait(levelUp ? 4200 : 3200);
  return result;
}

export function wait(ms: number): Promise<void> {
  return new Promise((r) => window.setTimeout(r, ms));
}

/** Roughly how long a young child needs to hear a line. */
export function readingTime(text: string): number {
  return Math.max(1400, Math.min(4200, text.length * 62));
}

function roundRng(parent: Rng): Rng {
  return createRng(Math.floor(parent.next() * 2 ** 31));
}

function shake(el: Element): void {
  el.classList.remove('shake');
  void (el as HTMLElement).offsetWidth;
  el.classList.add('shake');
}

function hash(s: string): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) h = Math.imul(h ^ s.charCodeAt(i), 16777619);
  return h >>> 0;
}
