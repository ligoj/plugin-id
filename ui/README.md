# plugin-id UI

Vue sources for the Ligoj "id" plugin. Built with Vite in library mode; the
output bundle is placed under the Java module's webjars classpath so the
Ligoj host serves it at `/webjars/id/vue/index.js`.

## Layout

```
ui/
├── package.json
├── vite.config.js            # library build → ../src/main/resources/.../webjars/id/vue/
├── index.html                # standalone dev entry
└── src/
    ├── index.js              # plugin contract entry (default export)
    ├── IdPlugin.vue          # root component
    └── service.js            # service / feature implementations
```

## Commands

```sh
npm install
npm run dev        # standalone dev server on :5174; proxies REST to :8080
npm run build      # writes ../src/main/resources/META-INF/resources/webjars/id/vue/index.js
```

`npm run dev` gives you the plugin in isolation — useful for UI work without
booting the full host app. The dev server proxies `/rest` and `/webjars` to
a locally running Ligoj backend on `:8080`.

## Shared dependencies

`vue`, `vue-router`, `pinia`, and `vuetify` are kept **external** in the
build output — the host resolves them via an import map so the plugin and
host share the same module instances. Without that, reactivity and
cross-component plugin registries break at SFC boundaries.

## Maven integration

Not wired yet. A follow-up will add `frontend-maven-plugin` so `mvn package`
runs `npm install && npm run build` automatically and the JAR ships with the
built bundle. For now, run `npm run build` manually before packaging.
