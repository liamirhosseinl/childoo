/**
 * Word content for the Word Lagoon games.
 * Emoji only from Unicode 11 or older so every Android 8+ phone renders them.
 */

export interface Word {
  word: string;
  emoji: string;
}

export const CVC: Word[] = [
  { word: 'cat', emoji: '🐱' }, { word: 'dog', emoji: '🐶' }, { word: 'sun', emoji: '☀️' },
  { word: 'hat', emoji: '🎩' }, { word: 'bus', emoji: '🚌' }, { word: 'pig', emoji: '🐷' },
  { word: 'fox', emoji: '🦊' }, { word: 'bed', emoji: '🛏️' }, { word: 'egg', emoji: '🥚' },
  { word: 'ant', emoji: '🐜' }, { word: 'bee', emoji: '🐝' }, { word: 'owl', emoji: '🦉' },
  { word: 'cow', emoji: '🐮' }, { word: 'car', emoji: '🚗' }, { word: 'key', emoji: '🔑' },
  { word: 'box', emoji: '📦' }, { word: 'map', emoji: '🗺️' }, { word: 'cup', emoji: '🥤' },
];

export const FOUR: Word[] = [
  { word: 'fish', emoji: '🐟' }, { word: 'frog', emoji: '🐸' }, { word: 'star', emoji: '⭐' },
  { word: 'moon', emoji: '🌙' }, { word: 'cake', emoji: '🎂' }, { word: 'ball', emoji: '⚽' },
  { word: 'duck', emoji: '🦆' }, { word: 'milk', emoji: '🥛' }, { word: 'bear', emoji: '🐻' },
  { word: 'lion', emoji: '🦁' }, { word: 'tree', emoji: '🌳' }, { word: 'ship', emoji: '🚢' },
  { word: 'kite', emoji: '🪁' }, { word: 'boat', emoji: '⛵' }, { word: 'sock', emoji: '🧦' },
  { word: 'bell', emoji: '🔔' }, { word: 'rain', emoji: '🌧️' }, { word: 'gift', emoji: '🎁' },
].filter((w) => w.word !== 'kite'); // 🪁 is Unicode 12; kept out for older phones.

export const LONG: Word[] = [
  { word: 'apple', emoji: '🍎' }, { word: 'house', emoji: '🏠' }, { word: 'horse', emoji: '🐴' },
  { word: 'train', emoji: '🚆' }, { word: 'pizza', emoji: '🍕' }, { word: 'robot', emoji: '🤖' },
  { word: 'zebra', emoji: '🦓' }, { word: 'tiger', emoji: '🐯' }, { word: 'snake', emoji: '🐍' },
  { word: 'whale', emoji: '🐋' }, { word: 'rocket', emoji: '🚀' }, { word: 'banana', emoji: '🍌' },
  { word: 'rabbit', emoji: '🐰' }, { word: 'monkey', emoji: '🐵' }, { word: 'turtle', emoji: '🐢' },
  { word: 'carrot', emoji: '🥕' }, { word: 'pencil', emoji: '✏️' }, { word: 'flower', emoji: '🌸' },
];

/** Words whose first letter is also their first sound (no silent or soft starts). */
export const FIRST_SOUND: Word[] = [
  { word: 'apple', emoji: '🍎' }, { word: 'ball', emoji: '⚽' }, { word: 'cat', emoji: '🐱' },
  { word: 'dog', emoji: '🐶' }, { word: 'egg', emoji: '🥚' }, { word: 'fish', emoji: '🐟' },
  { word: 'goat', emoji: '🐐' }, { word: 'hat', emoji: '🎩' }, { word: 'igloo', emoji: '🏠' },
{ word: 'lion', emoji: '🦁' }, { word: 'moon', emoji: '🌙' },
  { word: 'owl', emoji: '🦉' }, { word: 'pig', emoji: '🐷' },
  { word: 'rabbit', emoji: '🐰' }, { word: 'sun', emoji: '☀️' }, { word: 'tiger', emoji: '🐯' },
  { word: 'umbrella', emoji: '☂️' }, { word: 'van', emoji: '🚐' }, { word: 'whale', emoji: '🐋' },
  { word: 'zebra', emoji: '🦓' },
].filter((w) => w.word !== 'igloo'); // No reliable igloo emoji.

/** Rhyme families: every word in a family rhymes with the others. */
export const RHYMES: Word[][] = [
  [{ word: 'cat', emoji: '🐱' }, { word: 'hat', emoji: '🎩' }, { word: 'bat', emoji: '🦇' }, { word: 'rat', emoji: '🐀' }],
  [{ word: 'dog', emoji: '🐶' }, { word: 'frog', emoji: '🐸' }],
  [{ word: 'bee', emoji: '🐝' }, { word: 'tree', emoji: '🌳' }, { word: 'key', emoji: '🔑' }],
  [{ word: 'cake', emoji: '🎂' }, { word: 'snake', emoji: '🐍' }],
  [{ word: 'car', emoji: '🚗' }, { word: 'star', emoji: '⭐' }],
  [{ word: 'fox', emoji: '🦊' }, { word: 'box', emoji: '📦' }, { word: 'socks', emoji: '🧦' }],
  [{ word: 'ring', emoji: '💍' }, { word: 'king', emoji: '🤴' }],
  [{ word: 'moon', emoji: '🌙' }, { word: 'spoon', emoji: '🥄' }, { word: 'balloon', emoji: '🎈' }],
  [{ word: 'bear', emoji: '🐻' }, { word: 'pear', emoji: '🍐' }, { word: 'chair', emoji: '🪑' }],
  [{ word: 'clock', emoji: '⏰' }, { word: 'sock', emoji: '🧦' }, { word: 'rock', emoji: '🎸' }],
  [{ word: 'bell', emoji: '🔔' }, { word: 'shell', emoji: '🐚' }],
  [{ word: 'fish', emoji: '🐟' }, { word: 'dish', emoji: '🍽️' }],
  [{ word: 'boat', emoji: '⛵' }, { word: 'goat', emoji: '🐐' }, { word: 'coat', emoji: '🧥' }],
  [{ word: 'mouse', emoji: '🐭' }, { word: 'house', emoji: '🏠' }],
].map((family) => family.filter((w) => w.emoji !== '🪑' && w.word !== 'rock'));

export interface Category {
  name: string;
  /** "A banana is a fruit" → article + singular noun. */
  noun: string;
  items: Word[];
  level: 1 | 2 | 3;
}

const RAW_CATEGORIES: Category[] = [
  { name: 'animals', noun: 'an animal', level: 1, items: [
    { word: 'dog', emoji: '🐶' }, { word: 'cat', emoji: '🐱' }, { word: 'lion', emoji: '🦁' }, { word: 'pig', emoji: '🐷' }, { word: 'cow', emoji: '🐮' }, { word: 'monkey', emoji: '🐵' }] },
  { name: 'fruit', noun: 'a fruit', level: 1, items: [
    { word: 'apple', emoji: '🍎' }, { word: 'banana', emoji: '🍌' }, { word: 'grapes', emoji: '🍇' }, { word: 'orange', emoji: '🍊' }, { word: 'pear', emoji: '🍐' }, { word: 'cherries', emoji: '🍒' }] },
  { name: 'vehicles', noun: 'a vehicle', level: 1, items: [
    { word: 'car', emoji: '🚗' }, { word: 'bus', emoji: '🚌' }, { word: 'train', emoji: '🚆' }, { word: 'bike', emoji: '🚲' }, { word: 'truck', emoji: '🚚' }, { word: 'tractor', emoji: '🚜' }] },
  { name: 'clothes', noun: 'something to wear', level: 1, items: [
    { word: 'shirt', emoji: '👕' }, { word: 'socks', emoji: '🧦' }, { word: 'hat', emoji: '🎩' }, { word: 'dress', emoji: '👗' }, { word: 'shoe', emoji: '👟' }, { word: 'coat', emoji: '🧥' }] },
  { name: 'sea animals', noun: 'a sea animal', level: 2, items: [
    { word: 'fish', emoji: '🐟' }, { word: 'whale', emoji: '🐋' }, { word: 'octopus', emoji: '🐙' }, { word: 'crab', emoji: '🦀' }, { word: 'shark', emoji: '🦈' }, { word: 'dolphin', emoji: '🐬' }] },
  { name: 'farm animals', noun: 'a farm animal', level: 2, items: [
    { word: 'cow', emoji: '🐮' }, { word: 'pig', emoji: '🐷' }, { word: 'sheep', emoji: '🐑' }, { word: 'horse', emoji: '🐴' }, { word: 'chicken', emoji: '🐔' }, { word: 'goat', emoji: '🐐' }] },
  { name: 'vegetables', noun: 'a vegetable', level: 2, items: [
    { word: 'carrot', emoji: '🥕' }, { word: 'broccoli', emoji: '🥦' }, { word: 'corn', emoji: '🌽' }, { word: 'potato', emoji: '🥔' }, { word: 'cucumber', emoji: '🥒' }, { word: 'pepper', emoji: '🌶️' }] },
  { name: 'instruments', noun: 'a musical instrument', level: 2, items: [
    { word: 'guitar', emoji: '🎸' }, { word: 'drum', emoji: '🥁' }, { word: 'violin', emoji: '🎻' }, { word: 'trumpet', emoji: '🎺' }, { word: 'piano', emoji: '🎹' }, { word: 'saxophone', emoji: '🎷' }] },
  { name: 'things that fly', noun: 'something that flies', level: 3, items: [
    { word: 'bird', emoji: '🐦' }, { word: 'plane', emoji: '✈️' }, { word: 'butterfly', emoji: '🦋' }, { word: 'helicopter', emoji: '🚁' }, { word: 'bee', emoji: '🐝' }, { word: 'rocket', emoji: '🚀' }] },
  { name: 'cold things', noun: 'something cold', level: 3, items: [
    { word: 'snowman', emoji: '⛄' }, { word: 'ice cream', emoji: '🍦' }, { word: 'snowflake', emoji: '❄️' }, { word: 'penguin', emoji: '🐧' }, { word: 'ice cube', emoji: '🧊' }] },
  { name: 'hot things', noun: 'something hot', level: 3, items: [
    { word: 'fire', emoji: '🔥' }, { word: 'sun', emoji: '☀️' }, { word: 'volcano', emoji: '🌋' }, { word: 'hot pepper', emoji: '🌶️' }, { word: 'coffee', emoji: '☕' }] },
  { name: 'things with wheels', noun: 'something with wheels', level: 3, items: [
    { word: 'car', emoji: '🚗' }, { word: 'bike', emoji: '🚲' }, { word: 'skateboard', emoji: '🛹' }, { word: 'bus', emoji: '🚌' }, { word: 'scooter', emoji: '🛴' }] },
];

export const CATEGORIES: Category[] = RAW_CATEGORIES.map((c) => ({
  ...c,
  items: c.items.filter((w) => w.emoji !== '🧊' && w.emoji !== '🛹'),
}));

/** Pairs that exclude each other in odd-one-out, so the odd item is truly odd. */
export const CATEGORY_CLASHES: Record<string, string[]> = {
  'animals': ['sea animals', 'farm animals', 'things that fly', 'cold things'],
  'sea animals': ['animals', 'farm animals'],
  'farm animals': ['animals', 'sea animals'],
  'fruit': ['vegetables', 'cold things'],
  'vegetables': ['fruit', 'hot things'],
  'vehicles': ['things with wheels', 'things that fly'],
  'things with wheels': ['vehicles'],
  'things that fly': ['animals', 'vehicles'],
  'cold things': ['animals', 'fruit'],
  'hot things': ['vegetables'],
  'clothes': [],
  'instruments': [],
};

export const SIGHT_WORDS_1 = ['the', 'and', 'is', 'to', 'in', 'it', 'you', 'we', 'can', 'see', 'look', 'my', 'go', 'up', 'down', 'me', 'big', 'red', 'blue', 'one', 'two', 'play', 'yes', 'no'];

/** Look-alike pairs for the harder level: the child must read, not guess by shape. */
export const SIGHT_WORDS_2: [string, string][] = [
  ['was', 'saw'], ['on', 'no'], ['the', 'then'], ['of', 'off'], ['who', 'how'], ['form', 'from'],
  ['there', 'three'], ['what', 'want'], ['went', 'want'], ['here', 'where'], ['said', 'sad'], ['come', 'came'],
];
