import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";
import mdx from "@mdx-js/rollup";
import remarkGfm from "remark-gfm";
import rehypeSlug from "rehype-slug";
import remarkToc from "remark-toc";

// https://vitejs.dev/config/
export default defineConfig(async () => {
    return {
        assetsInclude: ["**/*.md"],
        optimizeDeps: {
            include: ["react/jsx-runtime"],
        },
        plugins: [
            react(),
            mdx({
                mdExtensions: [],
                providerImportSource: "@mdx-js/react",
                remarkPlugins: [remarkGfm, remarkToc],
                rehypePlugins: [rehypeSlug],
            }),
            svgr(),
        ],
        server: {
            open: true,
            // Proxy localhost/api to local prime-router
            proxy: {
                "/api": {
                    target: "http://127.0.0.1:7071",
                    changeOrigin: true,
                },
            },
        },
        build: {
            outDir: "build",
        },
        resolve: {
            alias: [{ find: /^~/, replacement: "./node_modules/" }],
        },
        css: {
            preprocessorOptions: {
                scss: {
                    includePaths: ["./node_modules/@uswds/uswds/packages"],
                },
            },
        },
    };
});
