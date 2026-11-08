#!/usr/bin/env bash
# Turns a green-screen character video into a transparent looping animated WebP for the games.
#
#   scripts/make-clip.sh <world> <state> <input.mp4> [trim-start-seconds] [duration-seconds]
#   scripts/make-clip.sh nuzo idle clips/nuzo-idle.mp4 0.2 3
#
# world: nuzo | lotli | embrix     state: idle | thinking | talk | happy | oops | celebrate
# Writes public/characters/<world>/<state>.webp and updates public/characters/manifest.json.
set -euo pipefail
world="$1"; state="$2"; input="$3"; start="${4:-0}"; dur="${5:-4}"
here="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$here/public/characters/$world"
mkdir -p "$out_dir"

ffmpeg -y -v error -ss "$start" -t "$dur" -i "$input" \
  -vf "chromakey=0x00B140:0.16:0.06,despill=type=green,crop=min(iw\,ih):min(iw\,ih),scale=512:512:flags=lanczos,fps=20" \
  -an -c:v libwebp_anim -lossless 0 -quality 72 -compression_level 6 -loop 0 \
  "$out_dir/$state.webp"

python3 - "$here/public/characters/manifest.json" "$world" "$state" <<'PY'
import json, sys
path, world, state = sys.argv[1:]
try:
    data = json.load(open(path))
except Exception:
    data = {}
states = set(data.get(world, []))
states.add(state)
data[world] = sorted(states)
json.dump(data, open(path, "w"), indent=2)
PY
echo "wrote $out_dir/$state.webp ($(du -h "$out_dir/$state.webp" | cut -f1))"
