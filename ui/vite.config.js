import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// Library build for the "id" plugin.
//
// Contract: the main Ligoj Vue host loads this plugin via a dynamic import of
//   /webjars/id/vue/index.js
// — so the output lives under the Java module's classpath resources, where
// Spring Boot's webjars servlet will serve it at runtime.
//
// Shared deps (vue, pinia, vue-router, vuetify) are kept EXTERNAL: the plugin
// must use the host's module instances or reactivity and plugin registries
// break across SFC boundaries. The host resolves these bare specifiers via an
// import map declared in its HTML entry points.

// Path to the Ligoj host repo, sitting beside `ligoj-plugins/` in the
// developer workspace. Used to resolve `@ligoj/host` for tests and the
// standalone dev server (runtime uses the host's import map).
const HOST_SRC = resolve(__dirname, '../../../ligoj/app-ui/src/main/webapp/src')

export default defineConfig({
  plugins: [vue()],

  resolve: {
    alias: {
      '@ligoj/host': resolve(HOST_SRC, 'host.js'),
      '@': HOST_SRC,
    },
    // Force a single instance of every shared dep so `setActivePinia`
    // from the test reaches `useI18nStore` resolved through `@ligoj/host`.
    // Without this each side picks its own node_modules copy.
    dedupe: ['vue', 'pinia', 'vue-router', 'vuetify'],
  },

  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.js'),
      formats: ['es'],
      fileName: () => 'index.js',
    },
    outDir: resolve(
      __dirname,
      '../src/main/resources/META-INF/resources/webjars/id/vue',
    ),
    emptyOutDir: true,
    rollupOptions: {
      external: ['vue', 'vue-router', 'pinia', 'vuetify', '@ligoj/host'],
      output: {
        assetFileNames: 'index.[ext]',
      },
    },
  },

  // Standalone dev server — tests the plugin in isolation against a running
  // Ligoj backend on :8080. `npm run dev` then open http://localhost:5174/.
  server: {
    port: 5174,
    proxy: {
      '/rest': { target: 'http://localhost:8080', changeOrigin: true },
      '/webjars': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },

  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['src/__tests__/setup.js'],
    exclude: ['node_modules/**', 'dist/**'],
    css: false,
    server: {
      deps: {
        inline: ['vuetify'],
      },
    },
  },
})
