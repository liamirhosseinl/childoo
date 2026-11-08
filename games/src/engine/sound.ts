import type { SfxName } from './types';

/**
 * Tiny synthesized sound effects (no audio files to ship or license).
 * Kept short and soft: they reward, they never startle.
 */
export class Sound {
  private ctx: AudioContext | null = null;

  constructor(private enabled: boolean) {}

  private audio(): AudioContext | null {
    if (!this.enabled) return null;
    if (!this.ctx) {
      const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Ctor) return null;
      this.ctx = new Ctor();
    }
    if (this.ctx.state === 'suspended') void this.ctx.resume();
    return this.ctx;
  }

  play(name: SfxName): void {
    const ctx = this.audio();
    if (!ctx) return;
    const t = ctx.currentTime;
    switch (name) {
      case 'tap':
        this.tone(ctx, t, 660, 0.06, 'triangle', 0.08);
        break;
      case 'tick':
        this.tone(ctx, t, 880, 0.04, 'sine', 0.06);
        break;
      case 'pop':
        this.sweep(ctx, t, 400, 900, 0.09, 'sine', 0.12);
        break;
      case 'whoosh':
        this.sweep(ctx, t, 300, 1200, 0.25, 'sine', 0.06);
        break;
      case 'correct':
        this.tone(ctx, t, 784, 0.12, 'triangle', 0.14);
        this.tone(ctx, t + 0.1, 1047, 0.18, 'triangle', 0.14);
        break;
      case 'wrong':
        this.tone(ctx, t, 330, 0.12, 'sine', 0.1);
        this.tone(ctx, t + 0.11, 294, 0.16, 'sine', 0.08);
        break;
      case 'star':
        this.sweep(ctx, t, 1200, 2000, 0.15, 'triangle', 0.08);
        break;
      case 'win':
        [523, 659, 784, 1047].forEach((f, i) => this.tone(ctx, t + i * 0.11, f, 0.22, 'triangle', 0.13));
        break;
    }
  }

  private tone(ctx: AudioContext, at: number, freq: number, dur: number, type: OscillatorType, vol: number): void {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, at);
    gain.gain.setValueAtTime(0.0001, at);
    gain.gain.exponentialRampToValueAtTime(vol, at + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, at + dur);
    osc.connect(gain).connect(ctx.destination);
    osc.start(at);
    osc.stop(at + dur + 0.02);
  }

  private sweep(ctx: AudioContext, at: number, from: number, to: number, dur: number, type: OscillatorType, vol: number): void {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(from, at);
    osc.frequency.exponentialRampToValueAtTime(to, at + dur);
    gain.gain.setValueAtTime(0.0001, at);
    gain.gain.exponentialRampToValueAtTime(vol, at + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, at + dur);
    osc.connect(gain).connect(ctx.destination);
    osc.start(at);
    osc.stop(at + dur + 0.02);
  }
}
