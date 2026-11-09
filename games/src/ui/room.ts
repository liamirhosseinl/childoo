import type { Host } from '../bridge';
import { CHARACTER_NAMES, activeBuddy, createMascot, dressMascot, isBuddyActive, portrait, setMood } from '../characters/mascots';
import { ITEMS, WORLDS, buy, levelFromXp, levelProgress, normalizeWallet, toggleEquip, type Item, type Slot, type Wallet } from '../engine/economy';
import type { Sound } from '../engine/sound';
import type { HostConfig, SessionResult, World } from '../engine/types';
import { escapeHtml } from './stage';

type Tab = 'buddies' | Slot;

const TABS: { id: Tab; label: string }[] = [
  { id: 'buddies', label: 'Buddies' },
  { id: 'hat', label: 'Hats' },
  { id: 'face', label: 'Glasses' },
  { id: 'back', label: 'Backpacks' },
];

/**
 * The Kid's Corner: the child's own space to see their buddy, spend coins and dress up.
 * No timers, no lessons here; it is the reward, and it is opened from the app, never during play.
 */
export function runRoom(mount: HTMLElement, config: HostConfig, host: Host, sound: Sound): Promise<SessionResult> {
  let wallet: Wallet = normalizeWallet(config.wallet);
  let companion: World = activeBuddy(config.companion);
  let tab: Tab = 'buddies';
  const started = performance.now();
  const say = (t: string) => config.voice !== false && host.speak(t);

  mount.innerHTML = `
    <div class="screen room" data-world="${companion}">
      <div class="sky" aria-hidden="true"></div>
      <header class="room-top">
        <h1 class="room-title">My buddy</h1>
        <div class="pill-stat coins"><span class="coin" aria-hidden="true"></span><b class="coin-count"></b></div>
        <div class="pill-stat streak" hidden>🔥 <b class="streak-count"></b></div>
      </header>
      <section class="room-hero">
        <div class="room-mascot"></div>
        <div class="room-name"></div>
        <div class="xp"><div class="xp-fill"></div></div>
        <div class="xp-label"></div>
      </section>
      <nav class="room-tabs"></nav>
      <main class="shelf"></main>
      <footer class="room-foot"><button type="button" class="done-btn">Done</button></footer>
      <div class="sheet" hidden></div>
    </div>`;
  const root = mount.querySelector('.room') as HTMLElement;
  const slot = root.querySelector('.room-mascot') as HTMLElement;
  const shelf = root.querySelector('.shelf') as HTMLElement;
  const tabs = root.querySelector('.room-tabs') as HTMLElement;
  const sheet = root.querySelector('.sheet') as HTMLElement;
  let mascot = createMascot(companion, wallet.equipped);
  slot.appendChild(mascot);

  const streak = config.streakDays ?? 0;
  if (streak >= 2) {
    (root.querySelector('.streak') as HTMLElement).hidden = false;
    (root.querySelector('.streak-count') as HTMLElement).textContent = String(streak);
  }

  const refreshStats = () => {
    (root.querySelector('.coin-count') as HTMLElement).textContent = String(wallet.coins);
    const level = levelFromXp(wallet.xp);
    (root.querySelector('.room-name') as HTMLElement).innerHTML = `${escapeHtml(CHARACTER_NAMES[companion])} <span>Level ${level}</span>`;
    (root.querySelector('.xp-fill') as HTMLElement).style.width = `${Math.round(levelProgress(wallet.xp) * 100)}%`;
    (root.querySelector('.xp-label') as HTMLElement).textContent = 'Play challenges to level up!';
  };

  const cheer = () => setMood(mascot, 'happy');

  const renderTabs = () => {
    tabs.innerHTML = '';
    TABS.forEach((t) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = `tab${t.id === tab ? ' on' : ''}`;
      b.textContent = t.label;
      b.addEventListener('click', () => {
        sound.play('tap');
        tab = t.id;
        renderTabs();
        renderShelf();
      });
      tabs.appendChild(b);
    });
  };

  const renderShelf = () => {
    shelf.innerHTML = '';
    if (tab === 'buddies') {
      [...WORLDS].sort((a, b) => Number(isBuddyActive(b)) - Number(isBuddyActive(a))).forEach((w) => {
        const active = isBuddyActive(w);
        const card = document.createElement('button');
        card.type = 'button';
        card.className = `shelf-card buddy${w === companion ? ' wearing' : ''}${active ? '' : ' soon'}`;
        card.innerHTML = `<div class="shelf-art"></div><b>${CHARACTER_NAMES[w]}</b><span>${
          w === companion ? 'Your buddy' : active ? 'Choose' : 'Coming soon'
        }</span>${active ? '' : '<i class="lock" aria-hidden="true"></i>'}`;
        (card.querySelector('.shelf-art') as HTMLElement).appendChild(portrait(w));
        card.addEventListener('click', () => {
          if (!active) {
            sound.play('tap');
            card.classList.remove('shake');
            void card.offsetWidth;
            card.classList.add('shake');
            toast(`${CHARACTER_NAMES[w]} is coming soon!`);
            return;
          }
          if (w === companion) {
            setMood(mascot, 'tap');
            return;
          }
          sound.play('pop');
          companion = w;
          root.dataset.world = w;
          slot.innerHTML = '';
          mascot = createMascot(companion, wallet.equipped);
          slot.appendChild(mascot);
          setMood(mascot, 'hello');
          say(`Hi! I'm ${CHARACTER_NAMES[w]}!`);
          refreshStats();
          renderShelf();
        });
        shelf.appendChild(card);
      });
      return;
    }
    ITEMS.filter((i) => i.slot === tab).forEach((item) => {
      const owned = wallet.owned.includes(item.id);
      const wearing = wallet.equipped[item.slot] === item.id;
      const card = document.createElement('button');
      card.type = 'button';
      card.className = `shelf-card${owned ? ' owned' : ''}${wearing ? ' wearing' : ''}${!owned && item.price > wallet.coins ? ' locked' : ''}`;
      card.innerHTML = `<div class="shelf-emoji">${item.emoji}</div><b>${escapeHtml(item.name)}</b><span>${
        wearing ? 'Wearing' : owned ? 'Put on' : `<i class="coin small"></i>${item.price}`
      }</span>`;
      card.addEventListener('click', () => onItem(item, card));
      shelf.appendChild(card);
    });
  };

  const onItem = (item: Item, card: HTMLElement) => {
    if (wallet.owned.includes(item.id)) {
      sound.play('pop');
      wallet = toggleEquip(wallet, item.id);
      dressMascot(mascot, companion, wallet.equipped);
      if (wallet.equipped[item.slot] === item.id) cheer();
      renderShelf();
      return;
    }
    if (item.price > wallet.coins) {
      sound.play('wrong');
      card.classList.remove('shake');
      void card.offsetWidth;
      card.classList.add('shake');
      const need = item.price - wallet.coins;
      toast(`${need} more coins to go. Keep playing!`);
      say(`You need ${need} more coins. Keep playing!`);
      return;
    }
    openSheet(item);
  };

  const openSheet = (item: Item) => {
    root.querySelectorAll('.toast').forEach((t) => t.remove());
    sheet.hidden = false;
    sheet.innerHTML = `
      <div class="sheet-card">
        <div class="shelf-emoji big">${item.emoji}</div>
        <h2>Get the ${escapeHtml(item.name.toLowerCase())}?</h2>
        <p><i class="coin small"></i>${item.price} coins</p>
        <div class="sheet-actions">
          <button type="button" class="sheet-no">Not now</button>
          <button type="button" class="sheet-yes">Yes!</button>
        </div>
      </div>`;
    say(`Get the ${item.name} for ${item.price} coins?`);
    (sheet.querySelector('.sheet-no') as HTMLElement).addEventListener('click', () => {
      sound.play('tap');
      sheet.hidden = true;
    });
    (sheet.querySelector('.sheet-yes') as HTMLElement).addEventListener('click', () => {
      const r = buy(wallet, item.id);
      sheet.hidden = true;
      if (!r.ok) return;
      wallet = r.wallet;
      sound.play('win');
      dressMascot(mascot, companion, wallet.equipped);
      cheer();
      say(`You got the ${item.name}! Looking great!`);
      refreshStats();
      renderShelf();
    });
  };

  const toast = (text: string) => {
    const t = document.createElement('div');
    t.className = 'toast';
    t.textContent = text;
    root.appendChild(t);
    window.setTimeout(() => t.remove(), 2200);
  };

  refreshStats();
  renderTabs();
  renderShelf();
  window.setTimeout(() => {
    setMood(mascot, 'hello');
    say(`Hi! It's me, ${CHARACTER_NAMES[companion]}. You have ${wallet.coins} coins.`);
  }, 400);

  return new Promise((resolve) => {
    (root.querySelector('.done-btn') as HTMLElement).addEventListener('click', () => {
      sound.play('whoosh');
      resolve({
        mode: 'room',
        gameId: 'room',
        skill: '',
        world: companion,
        level: 0,
        newLevel: 0,
        rounds: 0,
        firstTry: 0,
        wrong: 0,
        extraRounds: 0,
        guessing: false,
        durationMs: Math.round(performance.now() - started),
        coinsEarned: 0,
        companion,
        wallet,
      });
    });
  });
}
