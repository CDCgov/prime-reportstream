/// <reference types="vitest" />

import { resolve } from "path";

import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";
import mdx from "@mdx-js/rollup";
import rehypeSlug from "rehype-slug";
import { remarkMdxToc } from "remark-mdx-toc";
import remarkFrontmatter from "remark-frontmatter";
import remarkMdxFrontmatter from "remark-mdx-frontmatter";
import { checker } from "vite-plugin-checker";

// https://vitejs.dev/config/
export default defineConfig(() => {
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
                    remarkMdxToc,
                    remarkFrontmatter,
                    remarkMdxFrontmatter,
                ],
                rehypePlugins: [rehypeSlug],
            }),
            svgr(),
            checker({
                typescript: true,
                eslint: {
                    lintCommand:
                        'eslint "./src/**/*[!.test][!.stories].{ts,tsx}"',
                },
            }),
        ],
        server: {
            port: process.env.VITE_PORT,
            open: true,
            // Proxy localhost/api to local prime-router
            proxy: {
                "/api": {
                    target: process.env.VITE_PROXY_URL,
                    changeOrigin: true,
                },
            },
            headers: {
                "content-security-policy": process.env.VITE_CSP ?? "",
            },
        },
        build: {
            outDir: "build",
            target: "esnext",
            assetsDir: "assets/app",
            sourcemap: true,
            rollupOptions: {
                input: {
                    // Key alphabetical order is important, otherwise
                    // rollup will name the bundle something other
                    // than index
                    index: resolve(__dirname, "index.html"),
                    notfound: resolve(__dirname, "404.html"),
                    unsupportedBrowser: resolve(
                        __dirname,
                        "unsupported-browser.html",
                    ),
                },
            },
        },
        css: {
            preprocessorOptions: {
                scss: {
                    includePaths: ["node_modules/@uswds/uswds/packages"],
                },
            },
            devSourcemap: true,
        },
        test: {
            globals: true,
            environment: "jsdom",
            setupFiles: "./src/setupTests.ts",
            globalSetup: "./src/globalSetup.ts",
            include: [
                "./src/**/__tests__/**/*.[jt]s?(x)",
                "./src/**/?(*.)+(spec|test).[jt]s?(x)",
            ],
            css: false,
            coverage: {
                include: ["src/**/*.{js,jsx,ts,tsx}", "!src/**/*.d.ts"],
                provider: "istanbul",
                all: false,
                reporter: ["clover", "json", "lcov", "text"],
            },
            clearMocks: true, // TODO: re-evalulate this setting,
            server: {
                deps: {
                    // Allows for mocking peer dependencies these libraries import
                    inline: ["@trussworks/react-uswds"],
                },
            },
        },
        resolve: {
            alias: {
                "msw/native": resolve(
                    __dirname,
                    "./node_modules/msw/lib/native/index.mjs",
                ),
            },
        },
    };
});
