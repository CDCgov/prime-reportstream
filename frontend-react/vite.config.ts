import { resolve } from "path";

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";
import mdx from "@mdx-js/rollup";
import remarkGfm from "remark-gfm";
import rehypeSlug from "rehype-slug";
import { remarkMdxToc } from "remark-mdx-toc";
import remarkFrontmatter from "remark-frontmatter";
import remarkMdxFrontmatter from "remark-mdx-frontmatter";
import { checker } from "vite-plugin-checker";
import dotenv from "dotenv";

// Force usage of custom environment files
dotenv.config({ path: `.env.${process.env.NODE_ENV}` });

// https://vitejs.dev/config/
export default defineConfig(async () => {
    return {
        optimizeDeps: {
            include: ["react/jsx-runtime"],
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
                    remarkMdxFrontmatter,
                ],
                rehypePlugins: [rehypeSlug],
            }),
            svgr(),
            checker({
                // e.g. use TypeScript check
                typescript: true,
                eslint: {
                    // for example, lint .ts and .tsx
                    lintCommand: 'eslint "./src/**/*.{ts,tsx}"',
                },
            }),
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
            target: "esnext",
            assetsDir: "assets/app",
            sourcemap:
                process.env.NODE_ENV === "development" ||
                process.env.SOURCEMAPS === "true",
            rollupOptions: {
                input: {
                    // Key alphabetical order is important, otherwise
                    // rollup will name the bundle something other
                    // than index
                    index: resolve(__dirname, "index.html"),
                    notfound: resolve(__dirname, "404.html"),
                },
            },
        },
        css: {
            preprocessorOptions: {
                scss: {
                    includePaths: ["node_modules/@uswds/uswds/packages"],
                },
            },
        },
    };
});
