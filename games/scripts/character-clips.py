"""
Turns a character's green-screen animation videos into the transparent animated WebP clips
the games play, and updates public/characters/manifest.json.

    python3 scripts/character-clips.py scripts/clips/lotli.json <videos-dir>

The spec lists one entry per state:

    { "buddy": "lotli",
      "states": { "idle": { "video": "IDLE Breathing.mp4", "from": 6, "to": 61, "loop": true },
                  "oops": { "video": "SAD.mp4", "from": 22, "to": 62, "cutBelow": 808 } } }

"from"/"to" are frame numbers at 12 fps; "cutBelow" blanks everything under a row of the
540 × 960 working frame (for captions baked into a video).

Matting: a segmentation model gives a solid silhouette (it is not fooled by green light
spilling onto the character), and a green-screen key gives crisp edges around it.
Every clip is scaled so the standing pose has the same height and the feet sit on the same line,
so states swap without the character jumping around.

Needs: ffmpeg, Pillow, numpy, scipy, rembg (pip install "rembg[cpu]").
"""
import glob
import json
import os
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image
from scipy import ndimage as ndi

FPS = 12
CW, CH = 400, 520          # clip canvas
STAND = 360                # standing height on the canvas
BASE = CH - 14             # feet line
HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def key_alpha(rgb):
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    mx = np.maximum(r, b)
    gn = (g - mx) / np.maximum(g, 20) * 100
    t = np.clip((gn - 3) / 9, 0, 1)
    return 1 - t * t * (3 - 2 * t)


def matte(frame_path, session, cut_below=None):
    from rembg import remove
    rgb = np.asarray(Image.open(frame_path).convert('RGB')).astype(np.float32)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    mx = np.maximum(r, b)
    despilled = np.dstack([r, np.minimum(g, mx), b]).clip(0, 255).astype(np.uint8)
    seg = np.asarray(remove(Image.fromarray(despilled), session=session, only_mask=True)).astype(np.float32) / 255
    core = ndi.binary_fill_holes(seg > 0.5)
    lab, n = ndi.label(core)
    if n > 1:
        sizes = ndi.sum(core, lab, range(1, n + 1))
        core = np.isin(lab, [i + 1 for i, s in enumerate(sizes) if s > 0.03 * sizes.max()])
    inner = ndi.binary_erosion(core, iterations=5)
    outer = ndi.binary_dilation(core, iterations=5)
    alpha = np.where(inner, 1.0, np.where(outer, key_alpha(rgb), 0.0))
    if cut_below:
        alpha[cut_below:, :] = 0
    alpha = ndi.gaussian_filter(alpha, 0.6)
    g2 = np.where(g > mx, mx + (g - mx) * 0.1, g)
    return Image.fromarray(np.dstack([r, g2, b, alpha * 255]).clip(0, 255).astype(np.uint8), 'RGBA')


def placement(frames):
    tops, bots, cxs = [], [], []
    for im in frames:
        a = np.asarray(im.getchannel('A')).astype(np.float32) / 255
        ys, _ = np.nonzero(a > 0.3)
        tops.append(ys.min())
        bots.append(ys.max())
        cxs.append((a * np.arange(a.shape[1])).sum() / a.sum())
    tops, bots = np.array(tops), np.array(bots)
    base = np.median(bots)
    grounded = np.abs(bots - base) < 6
    height = np.median((bots - tops)[grounded]) if grounded.any() else np.median(bots - tops)
    return base, height, np.median(cxs)


def place(im, base, height, cx):
    s = STAND / height
    scaled = im.resize((round(im.width * s), round(im.height * s)), Image.LANCZOS)
    canvas = Image.new('RGBA', (CW, CH), (0, 0, 0, 0))
    canvas.paste(scaled, (round(CW / 2 - cx * s), round(BASE - base * s)), scaled)
    return canvas


def head_track(canvas):
    """Head centre x, head top y and head width, as fractions of the canvas."""
    a = np.asarray(canvas.getchannel('A')) > 80
    ys, _ = np.nonzero(a)
    if len(ys) == 0:
        return [0.5, 0.3, 0.5]
    top = ys.min()
    _, bx = np.nonzero(a[top:top + int(STAND * 0.35)])
    cx = float(np.median(bx))
    cy, _ = np.nonzero(a[:, max(0, int(cx) - 25):int(cx) + 25])
    head_top = cy.min() if len(cy) else top
    return [round(cx / CW, 3), round(float(head_top) / CH, 3), round(STAND * 0.62 / CW, 3)]


def seamless(frames, k=6):
    """Cross-fades the tail into the head so a loop has no visible seam."""
    n = len(frames)
    blended = [Image.blend(frames[n - k + i], frames[i], (i + 1) / (k + 1)) for i in range(k)]
    return blended + frames[k:n - k]


def main():
    spec_path, videos = sys.argv[1], sys.argv[2]
    spec = json.load(open(spec_path))
    buddy = spec['buddy']
    out_dir = os.path.join(HERE, 'public', 'characters', buddy)
    os.makedirs(out_dir, exist_ok=True)
    from rembg import new_session
    session = new_session('u2net')
    manifest_path = os.path.join(HERE, 'public', 'characters', 'manifest.json')
    try:
        manifest = json.load(open(manifest_path))
    except (OSError, ValueError):
        manifest = {}
    entry = {'w': CW, 'h': CH, 'box': round((STAND / 0.92) / CH, 3), 'states': {}}

    for state, s in spec['states'].items():
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', os.path.join(videos, s['video']),
                            '-vf', f'fps={FPS},scale=540:960', f'{tmp}/%03d.png'], check=True)
            paths = sorted(glob.glob(f'{tmp}/*.png'))[s['from']:s['to'] + 1]
            frames = [matte(p, session, s.get('cutBelow')) for p in paths]
        base, height, cx = placement(frames)
        placed = [place(f, base, height, cx) for f in frames]
        if s.get('loop'):
            placed = seamless(placed)
        duration = round(1000 / FPS)
        placed[0].save(os.path.join(out_dir, f'{state}.webp'), save_all=True, append_images=placed[1:],
                       duration=duration, loop=0 if s.get('loop') else 1,
                       quality=70, alpha_quality=85, method=5)
        entry['states'][state] = {'ms': len(placed) * duration, 'loop': bool(s.get('loop')), 'fps': FPS,
                                  'track': [head_track(c) for c in placed]}
        print(f'{state}: {len(placed)} frames', flush=True)

    manifest[buddy] = entry
    json.dump(manifest, open(manifest_path, 'w'), separators=(',', ':'))


if __name__ == '__main__':
    main()
