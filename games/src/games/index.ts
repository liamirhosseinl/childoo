import type { Game } from '../engine/types';
import { MATH_GAMES } from './math';
import { MIND_GAMES } from './mind';
import { WORD_GAMES } from './words';

/** Every game in the app. Add a game here and the planner starts using it. */
export const GAMES: Game[] = [...MATH_GAMES, ...WORD_GAMES, ...MIND_GAMES];
