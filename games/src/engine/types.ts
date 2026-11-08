import type { Rng } from './rng';

export type World = 'nuzo' | 'lotli' | 'embrix';
export type Mood = 'idle' | 'thinking' | 'happy' | 'oops' | 'cheer' | 'talk';

/** What the Android host passes in. Every field is optional so the engine always has a safe default. */
export interface HostConfig {
  age?: number;
  childName?: string;
  companion?: World;
  /** Current level per game id, kept by the host between sessions. */
  levels?: Record<string, number>;
  /** Most recent game ids first, to avoid repeats. */
  recent?: string[];
  seed?: number;
  /** Debug and QA: run this game regardless of the planner. */
  forceGame?: string;
  forceLevel?: number;
  sound?: boolean;
  voice?: boolean;
}

export interface Choice<T> {
  value: T;
  /** Visible label: text, a number or an emoji. */
  label: string;
  /** Optional spoken text when tapped or when read. */
  speak?: string;
  /** Extra CSS class for special tiles (e.g. 'word', 'emoji'). */
  kind?: 'number' | 'word' | 'emoji' | 'letter' | 'shape';
}

export interface ChooseOptions<T> {
  layout?: 'grid' | 'row' | 'column';
  /** Custom coaching for a specific wrong pick; may also trigger visual feedback. */
  coach?: (value: T) => string | undefined;
}

/** Everything a game round can use. The engine owns tracking, feedback and pacing. */
export interface RoundContext {
  readonly level: number;
  readonly index: number;
  readonly total: number;
  readonly age: number;
  readonly rng: Rng;
  /** The play area for this round; cleared before each round. */
  readonly play: HTMLElement;
  /** Shows the line in the speech bubble and reads it aloud. */
  prompt(text: string, spoken?: string): void;
  /** Reads a line aloud without changing the bubble. */
  say(text: string): void;
  /** Renders answer tiles and resolves with the first correct pick; wrong picks are tracked and coached. */
  choose<T>(choices: Choice<T>[], isCorrect: (value: T) => boolean, options?: ChooseOptions<T>): Promise<T>;
  /** For custom interactions: report a wrong attempt (optionally on an element to shake). */
  wrong(el?: Element | null, line?: string): void;
  /** For custom interactions: report the correct answer. */
  correct(line?: string): void;
  /** Speaks a short "why" after a correct answer and waits for it. Makes every game conceptual, not just drill. */
  explain(text: string): Promise<void>;
  mascot(mood: Mood): void;
  sfx(name: SfxName): void;
  wait(ms: number): Promise<void>;
}

export type SfxName = 'tap' | 'correct' | 'wrong' | 'pop' | 'star' | 'win' | 'whoosh' | 'tick';

export interface Game {
  id: string;
  title: string;
  world: World;
  /** Skill family for reports, e.g. 'math.counting'. */
  skill: string;
  minAge: number;
  maxAge: number;
  maxLevel: number;
  /** Rounds per session; the engine may add up to two when it detects guessing. */
  rounds: number;
  /** False for games where guessing is impossible (breathing, movement). */
  antiGuess?: boolean;
  /** Spoken once before the first round. */
  intro(level: number): string;
  round(ctx: RoundContext): Promise<void>;
}

export interface RoundRecord {
  /** Milliseconds from the round start to each wrong tap. */
  wrongAt: number[];
  solvedAt: number;
}

export interface SessionResult {
  gameId: string;
  skill: string;
  world: World;
  level: number;
  newLevel: number;
  rounds: number;
  firstTry: number;
  wrong: number;
  extraRounds: number;
  guessing: boolean;
  durationMs: number;
}
