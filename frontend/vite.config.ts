import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { codeInspectorPlugin } from 'code-inspector-plugin';
import path from 'path';

export default defineConfig(({ command }) => ({
  plugins: [
    react(),
    // Was in config-overrides.js only active for the dev server, and stays that way.
    ...(command === 'serve'
      ? [codeInspectorPlugin({ bundler: 'vite', editor: 'idea', hotKeys: ['altKey'] })]
      : [])
  ],

  resolve: {
    alias: [
      // The source uses absolute imports rooted at the project ("src/models/...").
      // CRA got that from tsconfig baseUrl; Vite needs it spelled out. Matched as a
      // regex on purpose - a plain "src" alias would also swallow package names that
      // merely start with those three letters.
      { find: /^src\//, replacement: path.resolve(__dirname, 'src') + '/' }
    ]
  },

  define: {
    // sockjs-client and a few other pre-bundler-era packages expect the Node global.
    global: 'globalThis'
  },

  build: {
    // The Dockerfile copies ./build into the nginx image. Keeping the CRA output
    // directory means the container stage needs no change at all.
    outDir: 'build',
    sourcemap: false
  },

  server: { port: 3000 },
  preview: { port: 3000 }
}));
