import {defineConfig, loadEnv, type ProxyOptions} from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

/**
 * The console talks to the core-ng service directly, so every backend path is proxied through the
 * dev server rather than called cross-origin. That keeps the browser on one origin and means the
 * backend needs no CORS configuration for local development.
 */
function createProxyConfig(target: string): Record<string, ProxyOptions> {
  const pathnames = ["/review", "/local-review", "/notification", "/settings", "/preset", "/service-config"];

  return pathnames.reduce(
    (config, pathname) => {
      config[pathname] = {target, changeOrigin: true, secure: false};
      return config;
    },
    {} as Record<string, ProxyOptions>
  );
}

export default defineConfig(({mode}) => {
  const viteEnv = loadEnv(mode, process.cwd(), "");
  const apiBase = viteEnv.VITE_API_BASE || "http://localhost:8080";

  return {
    base: viteEnv.VITE_PUBLIC_PATH || "/",
    publicDir: "public",

    plugins: [react()],

    esbuild: {
      target: "es2020",
    },

    build: {
      target: "es2020",
      outDir: path.resolve("build/dist"),
      rollupOptions: {
        input: {
          index: path.resolve(__dirname, "index.html"),
        },
        output: {
          entryFileNames: "static/js/[name].[hash].js",
          chunkFileNames: "static/js/[name].[hash].js",
          assetFileNames: "static/[name].[hash][extname]",
        },
      },
    },

    resolve: {
      alias: {
        "@": path.resolve(__dirname, "src"),
        type: path.resolve(__dirname, "src/type"),
      },
    },

    server: {
      // the console is a local tool with no login; it must not be reachable from the network
      host: "127.0.0.1",
      port: 7480,
      proxy: createProxyConfig(apiBase),
      hmr: true,
    },
  };
});
