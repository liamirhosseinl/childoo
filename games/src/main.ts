import './ui/styles.css';
import { createHost } from './bridge';
import { loadClipManifest } from './characters/clips';
import { createRng } from './engine/rng';
import { pickGame } from './engine/rules';
import { runSession } from './engine/session';
import { Sound } from './engine/sound';
import { Music } from './engine/music';
import { GAMES } from './games';
import { runRoom } from './ui/room';
import { Stage } from './ui/stage';

async function boot(): Promise<void> {
  const host = createHost();
  const config = host.config();
  const mount = document.getElementById('app') as HTMLElement;
  await loadClipManifest();
  const sound = new Sound(config.sound !== false);
  const music = new Music(config.music !== false && config.sound !== false);

  if (config.mode === 'room') {
    music.start(config.companion ?? 'nuzo');
    const result = await runRoom(mount, config, host, sound);
    music.stop();
    host.stopSpeech();
    host.complete(result);
    return;
  }

  const rng = createRng(config.seed ?? Date.now());
  const game = pickGame(GAMES, config, rng);
  const stage = new Stage(mount);
  music.start(game.world);
  const result = await runSession(game, config, { stage, sound, host });
  music.stop();
  host.stopSpeech();
  host.complete(result);
}

void boot();
