import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";

// https://vitejs.dev/config/
export default defineConfig(async () => {
    const mdx = await import("@mdx-js/rollup");
    const remarkGfm = await import("remark-gfm");
    const rehypeSlug = await import("rehype-slug");

    return {
        assetsInclude: ["**/*.md"],
        optimizeDeps: {
            include: ["react/jsx-runtime"],
        },
        plugins: [
            react(),
            mdx.default({
                mdExtensions: [],
                providerImportSource: "@mdx-js/react",
                remarkPlugins: [remarkGfm.default],
                rehypePlugins: [rehypeSlug.default],
            }),
            svgr(),
        ],
        server: {
            open: true,
            proxy: {
                "/api": {
                    target: "http://127.0.0.1:7071",
                    changeOrigin: true,
                    //rewrite: (path) => path.replace(/^\/api/, ""),
                },
            },
        },
        build: {
            outDir: "build",
        },
        resolve: {
            alias: [{ find: /^~/, replacement: "./node_modules/" }],
        },
    };
});
