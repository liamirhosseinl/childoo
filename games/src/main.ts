import './ui/styles.css';
import { createHost } from './bridge';
import { loadClipManifest } from './characters/clips';
import { activeBuddy } from './characters/mascots';
import { normalizeWallet } from './engine/economy';
import { Music } from './engine/music';
import { createRng } from './engine/rng';
import { defaultLevel, pickGame } from './engine/rules';
import { runSession } from './engine/session';
import { Sound } from './engine/sound';
import type { Game, HostConfig, World } from './engine/types';
import { GAMES } from './games';
import { pickIsland, type IslandInfo } from './ui/islands';
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
    const buddy = activeBuddy(config.companion);
    music.start(buddy);
    const result = await runRoom(mount, { ...config, companion: buddy }, host, sound);
    music.stop();
    host.stopSpeech();
    host.complete(result);
    return;
  }

  const stage = new Stage(mount);
  const speak = (t: string) => config.voice !== false && host.speak(t);

  // 1. A buddy is picked once, ever; afterwards it is remembered by the host.
  let companion: World | null = config.companion ? activeBuddy(config.companion) : null;
  if (!companion) {
    stage.setWorld(activeBuddy(null), '');
    companion = await stage.pickBuddy(speak, () => sound.play('pop'));
    sound.play('correct');
  }

  // 2. The child picks an island (unless QA forced a game).
  const age = config.age ?? 6;
  let chosenWorld: World | null = null;
  if (!config.forceGame) {
    music.start(companion);
    chosenWorld = await pickIsland(
      stage.root,
      islandInfo(GAMES, config),
      companion,
      normalizeWallet(config.wallet),
      config.streakDays ?? 0,
      () => sound.play('pop'),
    );
    music.stop();
  }

  // 3. A game from that island, fitted to age and history.
  const rng = createRng(config.seed ?? Date.now());
  const pool = chosenWorld ? GAMES.filter((g) => g.world === chosenWorld && age >= g.minAge && age <= g.maxAge) : GAMES;
  const game = pickGame(pool.length > 0 ? pool : GAMES, config, rng);
  music.start(game.world);
  const sessionConfig: HostConfig = { ...config, companion };
  const result = await runSession(game, sessionConfig, { stage, sound, host });
  music.stop();
  host.stopSpeech();
  host.complete({ ...result, companion, chosenWorld });

  function islandInfo(games: Game[], cfg: HostConfig): IslandInfo[] {
    return (['nuzo', 'lotli', 'embrix'] as World[]).map((world) => {
      const mine = games.filter((g) => g.world === world && age >= g.minAge && age <= g.maxAge);
      const levels = mine.map((g) => cfg.levels?.[g.id] ?? defaultLevel(age, g.maxLevel));
      const max = mine.reduce((n, g) => n + g.maxLevel, 0);
      const sum = levels.reduce((n, l) => n + l, 0);
      return {
        world,
        games: mine.length,
        level: Math.max(1, Math.round(sum / Math.max(1, mine.length))),
        progress: max === 0 ? 0 : sum / max,
      };
    });
  }
}

void boot();
