import './ui/styles.css';
import { createHost } from './bridge';
import { createRng } from './engine/rng';
import { pickGame } from './engine/rules';
import { runSession } from './engine/session';
import { Sound } from './engine/sound';
import { GAMES } from './games';
import { Stage } from './ui/stage';

async function boot(): Promise<void> {
  const host = createHost();
  const config = host.config();
  const rng = createRng(config.seed ?? Date.now());
  const game = pickGame(GAMES, config, rng);
  const stage = new Stage(document.getElementById('app') as HTMLElement);
  const sound = new Sound(config.sound !== false);
  const result = await runSession(game, config, { stage, sound, host });
  host.stopSpeech();
  host.complete(result);
}

void boot();
