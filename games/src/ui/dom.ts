/** Minimal DOM helpers so games stay readable without a framework. */
export function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className = '',
  html = '',
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (html) node.innerHTML = html;
  return node;
}

export function button(className: string, html: string, onTap: () => void): HTMLButtonElement {
  const b = el('button', className, html);
  b.type = 'button';
  b.addEventListener('click', () => {
    if (!b.disabled) onTap();
  });
  return b;
}

export function replay(node: Element, cls: string): void {
  node.classList.remove(cls);
  void (node as HTMLElement).offsetWidth;
  node.classList.add(cls);
}

export function plural(n: number, one: string, many: string): string {
  return n === 1 ? one : many;
}
