import type { World } from './types';

interface Theme {
  /** Scale as semitone offsets from the root (pentatonic: no clashing notes). */
  scale: number[];
  root: number;
  bpm: number;
  wave: OscillatorType;
}

const THEMES: Record<World, Theme> = {
  nuzo: { scale: [0, 2, 4, 7, 9], root: 62, bpm: 84, wave: 'sine' }, // spacey D major pentatonic
  lotli: { scale: [0, 2, 4, 7, 9], root: 65, bpm: 76, wave: 'triangle' }, // watery F
  embrix: { scale: [0, 3, 5, 7, 10], root: 57, bpm: 92, wave: 'triangle' }, // warm A minor pentatonic
};

/**
 * Quiet generated background music: a slow pentatonic melody over a soft two-note pad.
 * Low volume by design so it never competes with the voice. Pauses when the page is hidden.
 */
export class Music {
  private ctx: AudioContext | null = null;
  private master: GainNode | null = null;
  private timer = 0;
  private step = 0;
  private nextAt = 0;
  private theme: Theme = THEMES.nuzo;
  private melody: number[] = [];

  constructor(private readonly enabled: boolean) {
    document.addEventListener('visibilitychange', () => {
      if (!this.ctx) return;
      if (document.hidden) void this.ctx.suspend();
      else void this.ctx.resume();
    });
  }

  start(world: World): void {
    if (!this.enabled) return;
    const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Ctor) return;
    this.theme = THEMES[world];
    this.ctx = new Ctor();
    this.master = this.ctx.createGain();
    this.master.gain.value = 0;
    this.master.gain.linearRampToValueAtTime(0.05, this.ctx.currentTime + 2);
    this.master.connect(this.ctx.destination);
    this.melody = this.makeMelody();
    this.nextAt = this.ctx.currentTime + 0.3;
    this.timer = window.setInterval(() => this.schedule(), 120);
    // Autoplay rules: start on the first touch if the context begins suspended.
    const kick = () => {
      void this.ctx?.resume();
      document.removeEventListener('pointerdown', kick);
    };
    document.addEventListener('pointerdown', kick);
  }

  stop(): void {
    window.clearInterval(this.timer);
    const ctx = this.ctx;
    const master = this.master;
    if (!ctx || !master) return;
    master.gain.cancelScheduledValues(ctx.currentTime);
    master.gain.setValueAtTime(master.gain.value, ctx.currentTime);
    master.gain.linearRampToValueAtTime(0, ctx.currentTime + 0.4);
    window.setTimeout(() => void ctx.close(), 500);
    this.ctx = null;
    this.master = null;
  }

  private makeMelody(): number[] {
    // A gentle random walk over the scale, 16 steps, with rests.
    const notes: number[] = [];
    let idx = 2;
    for (let i = 0; i < 16; i++) {
      if (i % 4 === 3 && Math.random() < 0.6) {
        notes.push(-1);
        continue;
      }
      idx = Math.max(0, Math.min(this.theme.scale.length * 2 - 1, idx + Math.floor(Math.random() * 3) - 1));
      const octave = Math.floor(idx / this.theme.scale.length);
      notes.push(this.theme.root + this.theme.scale[idx % this.theme.scale.length] + 12 * octave);
    }
    return notes;
  }

  private schedule(): void {
    const ctx = this.ctx;
    if (!ctx || !this.master) return;
    const beat = 60 / this.theme.bpm / 2; // eighth notes
    while (this.nextAt < ctx.currentTime + 0.5) {
      const note = this.melody[this.step % this.melody.length];
      if (note >= 0) this.voice(note, this.nextAt, beat * 1.8, this.theme.wave, 0.5);
      if (this.step % 8 === 0) {
        const bar = Math.floor(this.step / 8) % 4;
        const root = this.theme.root - 12 + [0, 5, 7, 5][bar];
        this.voice(root, this.nextAt, beat * 8, 'sine', 0.35);
        this.voice(root + 7, this.nextAt, beat * 8, 'sine', 0.2);
      }
      this.step++;
      if (this.step % 64 === 0) this.melody = this.makeMelody();
      this.nextAt += beat;
    }
  }

  private voice(midi: number, at: number, dur: number, wave: OscillatorType, vol: number): void {
    const ctx = this.ctx;
    if (!ctx || !this.master) return;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = wave;
    osc.frequency.value = 440 * 2 ** ((midi - 69) / 12);
    gain.gain.setValueAtTime(0.0001, at);
    gain.gain.exponentialRampToValueAtTime(vol, at + Math.min(0.12, dur / 4));
    gain.gain.exponentialRampToValueAtTime(0.0001, at + dur);
    osc.connect(gain).connect(this.master);
    osc.start(at);
    osc.stop(at + dur + 0.05);
  }
}
