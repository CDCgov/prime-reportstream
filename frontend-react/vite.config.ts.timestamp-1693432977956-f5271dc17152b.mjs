// vite.config.ts
import { resolve } from "path";
import { defineConfig } from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/vite/dist/node/index.js";
import react from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/@vitejs/plugin-react/dist/index.mjs";
import svgr from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/vite-plugin-svgr/dist/index.js";
import mdx from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/@mdx-js/rollup/index.js";
import remarkGfm from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/remark-gfm/index.js";
import rehypeSlug from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/rehype-slug/index.js";
import { remarkMdxToc } from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/remark-mdx-toc/dist/index.js";
import remarkFrontmatter from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/remark-frontmatter/index.js";
import remarkMdxFrontmatter from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/remark-mdx-frontmatter/index.js";
import { checker } from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/vite-plugin-checker/dist/esm/main.js";
import dotenv from "file:///Users/erb/Development/prime-reportstream/frontend-react/node_modules/dotenv/lib/main.js";
var __vite_injected_original_dirname = "/Users/erb/Development/prime-reportstream/frontend-react";
dotenv.config({ path: `.env.${process.env.NODE_ENV}` });
var vite_config_default = defineConfig(async () => {
  return {
    optimizeDeps: {
      include: ["react/jsx-runtime"]
    },
    plugins: [
      react(),
      mdx({
        mdExtensions: [],
        providerImportSource: "@mdx-js/react",
        remarkPlugins: [
          remarkGfm,
          remarkMdxToc,
          remarkFrontmatter,
          remarkMdxFrontmatter
        ],
        rehypePlugins: [rehypeSlug]
      }),
      svgr(),
      checker({
        // e.g. use TypeScript check
        typescript: true,
        eslint: {
          // for example, lint .ts and .tsx
          lintCommand: 'eslint "./src/**/*.{ts,tsx}"'
        }
      })
    ],
    server: {
      open: true,
      // Proxy localhost/api to local prime-router
      proxy: {
        "/api": {
          target: "http://127.0.0.1:7071",
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: "build",
      target: "esnext",
      assetsDir: "assets/app",
      sourcemap: process.env.NODE_ENV === "development" || process.env.SOURCEMAPS === "true",
      rollupOptions: {
        input: {
          // Key alphabetical order is important, otherwise
          // rollup will name the bundle something other
          // than index
          index: resolve(__vite_injected_original_dirname, "index.html"),
          notfound: resolve(__vite_injected_original_dirname, "404.html")
        }
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          includePaths: ["node_modules/@uswds/uswds/packages"]
        }
      },
      devSourcemap: process.env.NODE_ENV === "development" || process.env.SOURCEMAPS === "true"
    }
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvVXNlcnMvZXJiL0RldmVsb3BtZW50L3ByaW1lLXJlcG9ydHN0cmVhbS9mcm9udGVuZC1yZWFjdFwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL1VzZXJzL2VyYi9EZXZlbG9wbWVudC9wcmltZS1yZXBvcnRzdHJlYW0vZnJvbnRlbmQtcmVhY3Qvdml0ZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL1VzZXJzL2VyYi9EZXZlbG9wbWVudC9wcmltZS1yZXBvcnRzdHJlYW0vZnJvbnRlbmQtcmVhY3Qvdml0ZS5jb25maWcudHNcIjtpbXBvcnQgeyByZXNvbHZlIH0gZnJvbSBcInBhdGhcIjtcblxuaW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSBcInZpdGVcIjtcbmltcG9ydCByZWFjdCBmcm9tIFwiQHZpdGVqcy9wbHVnaW4tcmVhY3RcIjtcbmltcG9ydCBzdmdyIGZyb20gXCJ2aXRlLXBsdWdpbi1zdmdyXCI7XG5pbXBvcnQgbWR4IGZyb20gXCJAbWR4LWpzL3JvbGx1cFwiO1xuaW1wb3J0IHJlbWFya0dmbSBmcm9tIFwicmVtYXJrLWdmbVwiO1xuaW1wb3J0IHJlaHlwZVNsdWcgZnJvbSBcInJlaHlwZS1zbHVnXCI7XG5pbXBvcnQgeyByZW1hcmtNZHhUb2MgfSBmcm9tIFwicmVtYXJrLW1keC10b2NcIjtcbmltcG9ydCByZW1hcmtGcm9udG1hdHRlciBmcm9tIFwicmVtYXJrLWZyb250bWF0dGVyXCI7XG5pbXBvcnQgcmVtYXJrTWR4RnJvbnRtYXR0ZXIgZnJvbSBcInJlbWFyay1tZHgtZnJvbnRtYXR0ZXJcIjtcbmltcG9ydCB7IGNoZWNrZXIgfSBmcm9tIFwidml0ZS1wbHVnaW4tY2hlY2tlclwiO1xuaW1wb3J0IGRvdGVudiBmcm9tIFwiZG90ZW52XCI7XG5cbi8vIEZvcmNlIHVzYWdlIG9mIGN1c3RvbSBlbnZpcm9ubWVudCBmaWxlc1xuZG90ZW52LmNvbmZpZyh7IHBhdGg6IGAuZW52LiR7cHJvY2Vzcy5lbnYuTk9ERV9FTlZ9YCB9KTtcblxuLy8gaHR0cHM6Ly92aXRlanMuZGV2L2NvbmZpZy9cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyhhc3luYyAoKSA9PiB7XG4gICAgcmV0dXJuIHtcbiAgICAgICAgb3B0aW1pemVEZXBzOiB7XG4gICAgICAgICAgICBpbmNsdWRlOiBbXCJyZWFjdC9qc3gtcnVudGltZVwiXSxcbiAgICAgICAgfSxcbiAgICAgICAgcGx1Z2luczogW1xuICAgICAgICAgICAgcmVhY3QoKSxcbiAgICAgICAgICAgIG1keCh7XG4gICAgICAgICAgICAgICAgbWRFeHRlbnNpb25zOiBbXSxcbiAgICAgICAgICAgICAgICBwcm92aWRlckltcG9ydFNvdXJjZTogXCJAbWR4LWpzL3JlYWN0XCIsXG4gICAgICAgICAgICAgICAgcmVtYXJrUGx1Z2luczogW1xuICAgICAgICAgICAgICAgICAgICByZW1hcmtHZm0sXG4gICAgICAgICAgICAgICAgICAgIHJlbWFya01keFRvYyxcbiAgICAgICAgICAgICAgICAgICAgcmVtYXJrRnJvbnRtYXR0ZXIsXG4gICAgICAgICAgICAgICAgICAgIHJlbWFya01keEZyb250bWF0dGVyLFxuICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgcmVoeXBlUGx1Z2luczogW3JlaHlwZVNsdWddLFxuICAgICAgICAgICAgfSksXG4gICAgICAgICAgICBzdmdyKCksXG4gICAgICAgICAgICBjaGVja2VyKHtcbiAgICAgICAgICAgICAgICAvLyBlLmcuIHVzZSBUeXBlU2NyaXB0IGNoZWNrXG4gICAgICAgICAgICAgICAgdHlwZXNjcmlwdDogdHJ1ZSxcbiAgICAgICAgICAgICAgICBlc2xpbnQ6IHtcbiAgICAgICAgICAgICAgICAgICAgLy8gZm9yIGV4YW1wbGUsIGxpbnQgLnRzIGFuZCAudHN4XG4gICAgICAgICAgICAgICAgICAgIGxpbnRDb21tYW5kOiAnZXNsaW50IFwiLi9zcmMvKiovKi57dHMsdHN4fVwiJyxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSksXG4gICAgICAgIF0sXG4gICAgICAgIHNlcnZlcjoge1xuICAgICAgICAgICAgb3BlbjogdHJ1ZSxcbiAgICAgICAgICAgIC8vIFByb3h5IGxvY2FsaG9zdC9hcGkgdG8gbG9jYWwgcHJpbWUtcm91dGVyXG4gICAgICAgICAgICBwcm94eToge1xuICAgICAgICAgICAgICAgIFwiL2FwaVwiOiB7XG4gICAgICAgICAgICAgICAgICAgIHRhcmdldDogXCJodHRwOi8vMTI3LjAuMC4xOjcwNzFcIixcbiAgICAgICAgICAgICAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICBidWlsZDoge1xuICAgICAgICAgICAgb3V0RGlyOiBcImJ1aWxkXCIsXG4gICAgICAgICAgICB0YXJnZXQ6IFwiZXNuZXh0XCIsXG4gICAgICAgICAgICBhc3NldHNEaXI6IFwiYXNzZXRzL2FwcFwiLFxuICAgICAgICAgICAgc291cmNlbWFwOlxuICAgICAgICAgICAgICAgIHByb2Nlc3MuZW52Lk5PREVfRU5WID09PSBcImRldmVsb3BtZW50XCIgfHxcbiAgICAgICAgICAgICAgICBwcm9jZXNzLmVudi5TT1VSQ0VNQVBTID09PSBcInRydWVcIixcbiAgICAgICAgICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgICAgICAgICAgICBpbnB1dDoge1xuICAgICAgICAgICAgICAgICAgICAvLyBLZXkgYWxwaGFiZXRpY2FsIG9yZGVyIGlzIGltcG9ydGFudCwgb3RoZXJ3aXNlXG4gICAgICAgICAgICAgICAgICAgIC8vIHJvbGx1cCB3aWxsIG5hbWUgdGhlIGJ1bmRsZSBzb21ldGhpbmcgb3RoZXJcbiAgICAgICAgICAgICAgICAgICAgLy8gdGhhbiBpbmRleFxuICAgICAgICAgICAgICAgICAgICBpbmRleDogcmVzb2x2ZShfX2Rpcm5hbWUsIFwiaW5kZXguaHRtbFwiKSxcbiAgICAgICAgICAgICAgICAgICAgbm90Zm91bmQ6IHJlc29sdmUoX19kaXJuYW1lLCBcIjQwNC5odG1sXCIpLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICBjc3M6IHtcbiAgICAgICAgICAgIHByZXByb2Nlc3Nvck9wdGlvbnM6IHtcbiAgICAgICAgICAgICAgICBzY3NzOiB7XG4gICAgICAgICAgICAgICAgICAgIGluY2x1ZGVQYXRoczogW1wibm9kZV9tb2R1bGVzL0B1c3dkcy91c3dkcy9wYWNrYWdlc1wiXSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIGRldlNvdXJjZW1hcDpcbiAgICAgICAgICAgICAgICBwcm9jZXNzLmVudi5OT0RFX0VOViA9PT0gXCJkZXZlbG9wbWVudFwiIHx8XG4gICAgICAgICAgICAgICAgcHJvY2Vzcy5lbnYuU09VUkNFTUFQUyA9PT0gXCJ0cnVlXCIsXG4gICAgICAgIH0sXG4gICAgfTtcbn0pO1xuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUEwVixTQUFTLGVBQWU7QUFFbFgsU0FBUyxvQkFBb0I7QUFDN0IsT0FBTyxXQUFXO0FBQ2xCLE9BQU8sVUFBVTtBQUNqQixPQUFPLFNBQVM7QUFDaEIsT0FBTyxlQUFlO0FBQ3RCLE9BQU8sZ0JBQWdCO0FBQ3ZCLFNBQVMsb0JBQW9CO0FBQzdCLE9BQU8sdUJBQXVCO0FBQzlCLE9BQU8sMEJBQTBCO0FBQ2pDLFNBQVMsZUFBZTtBQUN4QixPQUFPLFlBQVk7QUFabkIsSUFBTSxtQ0FBbUM7QUFlekMsT0FBTyxPQUFPLEVBQUUsTUFBTSxRQUFRLFFBQVEsSUFBSSxRQUFRLEdBQUcsQ0FBQztBQUd0RCxJQUFPLHNCQUFRLGFBQWEsWUFBWTtBQUNwQyxTQUFPO0FBQUEsSUFDSCxjQUFjO0FBQUEsTUFDVixTQUFTLENBQUMsbUJBQW1CO0FBQUEsSUFDakM7QUFBQSxJQUNBLFNBQVM7QUFBQSxNQUNMLE1BQU07QUFBQSxNQUNOLElBQUk7QUFBQSxRQUNBLGNBQWMsQ0FBQztBQUFBLFFBQ2Ysc0JBQXNCO0FBQUEsUUFDdEIsZUFBZTtBQUFBLFVBQ1g7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxRQUNKO0FBQUEsUUFDQSxlQUFlLENBQUMsVUFBVTtBQUFBLE1BQzlCLENBQUM7QUFBQSxNQUNELEtBQUs7QUFBQSxNQUNMLFFBQVE7QUFBQTtBQUFBLFFBRUosWUFBWTtBQUFBLFFBQ1osUUFBUTtBQUFBO0FBQUEsVUFFSixhQUFhO0FBQUEsUUFDakI7QUFBQSxNQUNKLENBQUM7QUFBQSxJQUNMO0FBQUEsSUFDQSxRQUFRO0FBQUEsTUFDSixNQUFNO0FBQUE7QUFBQSxNQUVOLE9BQU87QUFBQSxRQUNILFFBQVE7QUFBQSxVQUNKLFFBQVE7QUFBQSxVQUNSLGNBQWM7QUFBQSxRQUNsQjtBQUFBLE1BQ0o7QUFBQSxJQUNKO0FBQUEsSUFDQSxPQUFPO0FBQUEsTUFDSCxRQUFRO0FBQUEsTUFDUixRQUFRO0FBQUEsTUFDUixXQUFXO0FBQUEsTUFDWCxXQUNJLFFBQVEsSUFBSSxhQUFhLGlCQUN6QixRQUFRLElBQUksZUFBZTtBQUFBLE1BQy9CLGVBQWU7QUFBQSxRQUNYLE9BQU87QUFBQTtBQUFBO0FBQUE7QUFBQSxVQUlILE9BQU8sUUFBUSxrQ0FBVyxZQUFZO0FBQUEsVUFDdEMsVUFBVSxRQUFRLGtDQUFXLFVBQVU7QUFBQSxRQUMzQztBQUFBLE1BQ0o7QUFBQSxJQUNKO0FBQUEsSUFDQSxLQUFLO0FBQUEsTUFDRCxxQkFBcUI7QUFBQSxRQUNqQixNQUFNO0FBQUEsVUFDRixjQUFjLENBQUMsb0NBQW9DO0FBQUEsUUFDdkQ7QUFBQSxNQUNKO0FBQUEsTUFDQSxjQUNJLFFBQVEsSUFBSSxhQUFhLGlCQUN6QixRQUFRLElBQUksZUFBZTtBQUFBLElBQ25DO0FBQUEsRUFDSjtBQUNKLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
