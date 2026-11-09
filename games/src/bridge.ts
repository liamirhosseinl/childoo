import type { HostConfig, SessionResult } from './engine/types';

/**
 * The Android side exposes `window.ChildooHost` (a @JavascriptInterface object).
 * In a desktop browser there is no host, so a fallback keeps the games playable for development and tests.
 */
interface NativeHost {
  getConfig(): string;
  complete(resultJson: string): void;
  speak(text: string): void;
  stopSpeech(): void;
  haptic(kind: string): void;
}

declare global {
  interface Window {
    ChildooHost?: NativeHost;
    __childooResult?: SessionResult;
  }
}

export interface Host {
  config(): HostConfig;
  complete(result: SessionResult): void;
  speak(text: string): void;
  stopSpeech(): void;
  haptic(kind: 'light' | 'success' | 'error'): void;
}

export function createHost(): Host {
  const native = window.ChildooHost;
  if (native) {
    return {
      config: () => safeParse(native.getConfig()),
      complete: (r) => native.complete(JSON.stringify(r)),
      speak: (t) => native.speak(t),
      stopSpeech: () => native.stopSpeech(),
      haptic: (k) => native.haptic(k),
    };
  }
  return browserHost();
}

function browserHost(): Host {
  const params = new URLSearchParams(location.search);
  const num = (k: string) => (params.has(k) ? Number(params.get(k)) : undefined);
  const config: HostConfig = {
    age: num('age') ?? 6,
    forceGame: params.get('game') ?? undefined,
    forceLevel: num('level'),
    seed: num('seed'),
    voice: params.get('voice') !== '0',
    sound: params.get('sound') !== '0',
    music: params.get('music') !== '0',
    mode: params.get('mode') === 'room' ? 'room' : 'challenge',
    companion: (['nuzo', 'lotli', 'embrix'] as const).find((w) => w === params.get('buddy')),
    streakDays: num('streak'),
    wallet: params.has('coins')
      ? { coins: num('coins') ?? 0, xp: num('xp') ?? num('coins') ?? 0, owned: (params.get('owned') ?? '').split(',').filter(Boolean), equipped: Object.fromEntries((params.get('wear') ?? '').split(',').filter((x) => x.includes(':')).map((x) => x.split(':'))) }
      : undefined,
  };
  const synth = typeof speechSynthesis !== 'undefined' ? speechSynthesis : undefined;
  return {
    config: () => config,
    complete: (r) => {
      window.__childooResult = r;
      document.dispatchEvent(new CustomEvent('childoo:complete', { detail: r }));
    },
    speak: (t) => {
      if (!synth || !config.voice) return;
      synth.cancel();
      const u = new SpeechSynthesisUtterance(t);
      u.lang = 'en-US';
      u.rate = 0.95;
      u.pitch = 1.1;
      synth.speak(u);
    },
    stopSpeech: () => synth?.cancel(),
    haptic: (k) => navigator.vibrate?.(k === 'error' ? [30, 40, 30] : k === 'success' ? 40 : 10),
  };
}

function safeParse(json: string): HostConfig {
  try {
    const v = JSON.parse(json);
    return typeof v === 'object' && v !== null ? (v as HostConfig) : {};
  } catch {
    return {};
  }
}
