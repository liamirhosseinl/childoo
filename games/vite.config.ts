import { defineConfig } from 'vite';

// Built into the Android game host's assets and served offline through WebViewAssetLoader.
export default defineConfig({
  base: './',
  build: {
    outDir: '../gamehost/src/main/assets/games',
    emptyOutDir: true,
    target: 'es2020',
    assetsInlineLimit: 0,
    sourcemap: false,
    // Stable names keep diffs readable when the bundle is committed.
    rollupOptions: {
      output: {
        entryFileNames: 'assets/game.js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name][extname]',
      },
    },
  },
  test: {
    environment: 'jsdom',
  },
});
