"""
Plays every game end to end in headless Chrome with a brute-force "child" and
saves screenshots: intro, mid-game and celebration. Fails on any console error
or a game that never finishes.

usage: python3 scripts/qa.py <built-dir> <out-dir> [game-id ...]
"""
import http.server
import json
import socketserver
import sys
import threading
import time
from functools import partial
from pathlib import Path

from playwright.sync_api import sync_playwright

GAMES = {
    'star-counter': [1, 3, 4], 'rocket-fuel': [2, 4], 'space-scale': [1, 3], 'moon-jump': [1, 3],
    'fair-share': [2], 'pattern-path': [1, 4], 'bubble-letters': [2], 'word-builder': [1, 3],
    'rhyme-lagoon': [1], 'odd-one-out': [1, 3], 'word-fishing': [2], 'dragon-breath': [1],
    'power-moves': [1], 'dragon-memory': [2],
    'space-hockey': [1, 4], 'number-snake': [1, 3], 'word-snake': [2], 'penalty-kick': [2, 4],
}

AUTOPLAY = r"""
async () => {
  const sleep = (ms) => new Promise(r => setTimeout(r, ms));
  let i = 0;
  const start = Date.now();
  while (Date.now() - start < 120000) {
    if (window.__childooResult) return window.__childooResult;
    const q = (s) => Array.from(document.querySelectorAll(s));
    const isle = document.querySelector('.islands:not(.leaving) .island:not(.picked)');
    if (isle && !document.querySelector('.island.picked')) { q('.island')[1].click(); await sleep(1100); continue; }
    const picker = document.querySelector('.picker:not(.leaving)');
    if (picker) {
      q('.buddy-card')[1].click(); await sleep(300);
      document.querySelector('.picker-go').click(); await sleep(600);
      continue;
    }
    const tiles = q('.tray .tile:not(:disabled)');
    if (tiles.length && !q('.tray .tile.right').length) { tiles[0].click(); await sleep(450); continue; }
    const letters = q('.letter-tile:not(:disabled)');
    if (letters.length) { letters[i++ % letters.length].click(); await sleep(250); continue; }
    const ticks = q('.tick-hit');
    if (ticks.length && !q('.tick.found').length) { ticks[i++ % ticks.length].click(); await sleep(250); continue; }
    const dishes = q('.plate-dish');
    if (dishes.length && q('.jar-cookies i').length) { dishes[i++ % dishes.length].click(); await sleep(120); continue; }
    const fish = q('.fish:not(:disabled)');
    if (fish.length && !q('.fish.caught').length) { fish[0].click(); await sleep(450); continue; }
    const breath = document.querySelector('.breath-btn:not(:disabled)');
    if (breath) {
      breath.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }));
      await sleep(3300);
      breath.dispatchEvent(new PointerEvent('pointerup', { bubbles: true }));
      await sleep(300);
      continue;
    }
    const move = document.querySelector('.move-tap');
    if (move) { move.click(); await sleep(150); continue; }
    const cards = q('.mem-card:not(.matched):not(.flipped)');
    if (cards.length >= 2) {
      const face = (c) => c.querySelector('.mem-face').textContent;
      const a = cards[0];
      const b = cards.slice(1).find(c => face(c) === face(a));
      a.click(); await sleep(120); b.click(); await sleep(500);
      continue;
    }
    await sleep(200);
  }
  return null;
}
"""


def serve(directory: str) -> int:
    handler = partial(http.server.SimpleHTTPRequestHandler, directory=directory)
    handler.log_message = lambda *a, **k: None
    httpd = socketserver.TCPServer(('127.0.0.1', 0), handler)
    threading.Thread(target=httpd.serve_forever, daemon=True).start()
    return httpd.server_address[1]


def main() -> int:
    built, out = sys.argv[1], Path(sys.argv[2])
    only = sys.argv[3:]
    out.mkdir(parents=True, exist_ok=True)
    port = serve(built)
    failures = []
    results = {}
    with sync_playwright() as p:
        browser = p.chromium.launch(executable_path='/usr/bin/google-chrome', args=['--autoplay-policy=no-user-gesture-required'])
        for game, levels in GAMES.items():
            if only and game not in only:
                continue
            for level in levels:
                page = browser.new_page(viewport={'width': 393, 'height': 852}, device_scale_factor=2, has_touch=False)
                errors = []
                page.on('console', lambda m: errors.append(m.text) if m.type == 'error' else None)
                page.on('pageerror', lambda e: errors.append(str(e)))
                buddy = '' if game == 'star-counter' and level == 1 else '&buddy=embrix'
                page.add_init_script('window.__qaSolve = true')
                page.goto(f'http://127.0.0.1:{port}/index.html?game={game}&level={level}&seed=7&voice=0&sound=0&coins=70&xp=70&streak=3{buddy}')
                page.wait_for_timeout(600)
                page.screenshot(path=str(out / f'{game}-L{level}-1-intro.png'))
                page.wait_for_timeout(3600)
                page.screenshot(path=str(out / f'{game}-L{level}-2-play.png'))
                t0 = time.time()
                result = page.evaluate(AUTOPLAY)
                page.wait_for_timeout(100)
                if result is None:
                    failures.append(f'{game} L{level}: did not finish')
                page.screenshot(path=str(out / f'{game}-L{level}-3-end.png'))
                if errors:
                    failures.append(f'{game} L{level}: console errors: {errors[:3]}')
                results[f'{game}-L{level}'] = {'result': result, 'seconds': round(time.time() - t0, 1)}
                page.close()
        if not only or 'islands' in only:
            page = browser.new_page(viewport={'width': 393, 'height': 852}, device_scale_factor=2)
            errors = []
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(f'http://127.0.0.1:{port}/index.html?voice=0&sound=0&coins=140&xp=140&streak=5&buddy=lotli&age=7')
            page.wait_for_timeout(1200)
            page.screenshot(path=str(out / 'islands-1.png'))
            res = page.evaluate(AUTOPLAY)
            page.screenshot(path=str(out / 'islands-2-end.png'))
            results['islands'] = res
            if not res or res.get('chosenWorld') != 'lotli':
                failures.append(f'islands: unexpected {res}')
            if errors:
                failures.append(f'islands: errors {errors[:3]}')
            page.close()
        if not only or 'room' in only:
            page = browser.new_page(viewport={'width': 393, 'height': 852}, device_scale_factor=2)
            errors = []
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(f'http://127.0.0.1:{port}/index.html?mode=room&voice=0&sound=0&coins=120&xp=230&owned=cap&buddy=nuzo&streak=4')
            page.wait_for_timeout(900)
            page.screenshot(path=str(out / 'room-1-buddies.png'))
            page.click('.room-tabs .tab:nth-child(2)')
            page.wait_for_timeout(300)
            page.click('.shelf-card:nth-child(4)')  # crown: too expensive
            page.wait_for_timeout(300)
            page.screenshot(path=str(out / 'room-2-hats-locked.png'))
            page.click('.shelf-card:nth-child(2)')  # top hat
            page.wait_for_timeout(400)
            page.screenshot(path=str(out / 'room-3-sheet.png'))
            page.click('.sheet-yes')
            page.wait_for_timeout(700)
            page.click('.room-tabs .tab:nth-child(3)')
            page.wait_for_timeout(200)
            page.screenshot(path=str(out / 'room-4-bought.png'))
            page.click('.done-btn')
            page.wait_for_timeout(300)
            room = page.evaluate('window.__childooResult')
            results['room'] = room
            if not room or room['wallet']['coins'] != 75 or 'top-hat' not in room['wallet']['owned']:
                failures.append(f'room: unexpected result {room}')
            if errors:
                failures.append(f'room: errors {errors[:3]}')
            page.close()
        browser.close()
    print(json.dumps(results, indent=1))
    if failures:
        print('FAILURES:\n' + '\n'.join(failures))
        return 1
    print('ALL GAMES PASSED')
    return 0


if __name__ == '__main__':
    sys.exit(main())
