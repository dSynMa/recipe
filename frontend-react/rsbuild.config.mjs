import { defineConfig } from '@rsbuild/core';
import { pluginReact } from '@rsbuild/plugin-react';

export default defineConfig({
  plugins: [pluginReact()],
  html: { template: './index.html' },
  output: { distPath: { root: 'build' } },
  server: { host: 'localhost', port: 3000, strictPort: true },
});
