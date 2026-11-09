/**
 * A crisp full-size canvas inside the play area with a frame loop and pointer tracking.
 * Shared by the arcade games so each one is only its own rules and drawing.
 */
export interface Pointer {
  x: number;
  y: number;
  down: boolean;
  /** Velocity in px/s, smoothed. */
  vx: number;
  vy: number;
}

export interface Arena {
  readonly canvas: HTMLCanvasElement;
  readonly ctx: CanvasRenderingContext2D;
  readonly w: number;
  readonly h: number;
  readonly pointer: Pointer;
  /** Starts the loop; the callback gets seconds since the last frame (capped). */
  run(frame: (dt: number) => void): void;
  stop(): void;
  onDown(cb: (p: Pointer) => void): void;
  onUp(cb: (p: Pointer) => void): void;
}

export function createArena(parent: HTMLElement): Arena {
  const rect = parent.getBoundingClientRect();
  const w = Math.max(200, Math.floor(rect.width));
  const h = Math.max(240, Math.floor(rect.height));
  const dpr = Math.min(window.devicePixelRatio || 1, 2.5);
  const canvas = document.createElement('canvas');
  canvas.className = 'arena';
  canvas.width = Math.floor(w * dpr);
  canvas.height = Math.floor(h * dpr);
  canvas.style.width = `${w}px`;
  canvas.style.height = `${h}px`;
  parent.appendChild(canvas);
  const ctx = canvas.getContext('2d') as CanvasRenderingContext2D;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

  const pointer: Pointer = { x: w / 2, y: h * 0.85, down: false, vx: 0, vy: 0 };
  const downs: ((p: Pointer) => void)[] = [];
  const ups: ((p: Pointer) => void)[] = [];
  let lastMove = performance.now();

  const local = (e: PointerEvent) => {
    const r = canvas.getBoundingClientRect();
    return { x: e.clientX - r.left, y: e.clientY - r.top };
  };
  canvas.addEventListener('pointerdown', (e) => {
    canvas.setPointerCapture(e.pointerId);
    const p = local(e);
    pointer.x = p.x;
    pointer.y = p.y;
    pointer.vx = 0;
    pointer.vy = 0;
    pointer.down = true;
    lastMove = performance.now();
    downs.forEach((cb) => cb(pointer));
  });
  canvas.addEventListener('pointermove', (e) => {
    const p = local(e);
    const now = performance.now();
    const dt = Math.max(1, now - lastMove) / 1000;
    pointer.vx = pointer.vx * 0.5 + ((p.x - pointer.x) / dt) * 0.5;
    pointer.vy = pointer.vy * 0.5 + ((p.y - pointer.y) / dt) * 0.5;
    pointer.x = p.x;
    pointer.y = p.y;
    lastMove = now;
  });
  const up = () => {
    if (!pointer.down) return;
    pointer.down = false;
    ups.forEach((cb) => cb(pointer));
  };
  canvas.addEventListener('pointerup', up);
  canvas.addEventListener('pointercancel', up);

  let raf = 0;
  let running = false;
  return {
    canvas,
    ctx,
    w,
    h,
    pointer,
    run(frame) {
      running = true;
      let last = performance.now();
      const tick = (now: number) => {
        if (!running) return;
        const dt = Math.min(0.033, (now - last) / 1000);
        last = now;
        // Pointer velocity decays when the finger rests.
        if (now - lastMove > 60) {
          pointer.vx *= 0.6;
          pointer.vy *= 0.6;
        }
        frame(dt);
        raf = requestAnimationFrame(tick);
      };
      raf = requestAnimationFrame(tick);
    },
    stop() {
      running = false;
      cancelAnimationFrame(raf);
    },
    onDown: (cb) => downs.push(cb),
    onUp: (cb) => ups.push(cb),
  };
}

/** Little bursts of dots for hits and goals. */
export class Sparks {
  private items: { x: number; y: number; vx: number; vy: number; life: number; color: string; r: number }[] = [];

  burst(x: number, y: number, color: string, count = 14, speed = 220): void {
    for (let i = 0; i < count; i++) {
      const a = (Math.PI * 2 * i) / count + Math.random() * 0.4;
      const s = speed * (0.5 + Math.random() * 0.7);
      this.items.push({ x, y, vx: Math.cos(a) * s, vy: Math.sin(a) * s, life: 1, color, r: 2 + Math.random() * 3 });
    }
  }

  update(dt: number): void {
    for (const p of this.items) {
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vx *= 0.94;
      p.vy *= 0.94;
      p.life -= dt * 1.6;
    }
    this.items = this.items.filter((p) => p.life > 0);
  }

  draw(ctx: CanvasRenderingContext2D): void {
    for (const p of this.items) {
      ctx.globalAlpha = Math.max(0, p.life);
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalAlpha = 1;
  }
}

export function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number): void {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

export function label(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, size: number, color = '#fff'): void {
  ctx.font = `700 ${size}px Fredoka, Nunito, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillStyle = color;
  ctx.fillText(text, x, y);
}
