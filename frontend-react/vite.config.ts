/// <reference types="vitest" />

import path, { resolve } from "path";

import { defineConfig, loadEnv, Plugin } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";
import mdx from "@mdx-js/rollup";
import rehypeSlug from "rehype-slug";
import { remarkMdxToc } from "remark-mdx-toc";
import remarkFrontmatter from "remark-frontmatter";
import remarkMdxFrontmatter from "remark-mdx-frontmatter";
import { checker } from "vite-plugin-checker";
import pnpUswdsPackages from "./pnp-uswds-packages";

const LOCAL_MODES = ["preview", "development", "test", "csp"];
const DEMO_MODES = /^demo\d+$/;
const TRIALFRONTEND_MODES = /^trialfrontend\d+$/;

/**
 * Determine the backend url based on mode.
 */
function createBackendUrl(mode: string) {
    const port = getPort(mode);
    if (LOCAL_MODES.includes(mode))
        return `http://localhost${port ? ":" + port.toString() : ""}`;
    return `https://${mode !== "production" ? mode + "." : ""}prime.cdc.gov`;
}

function getPort(mode: string) {
    switch (mode) {
        case "csp":
        case "preview":
            return 4173;
        default:
            return 3000;
    }
}

/**
 * Simplify known number-ranged modes to base mode (ex: trialfrontend01 -> trialfrontend)
 */
function loadRedirectedEnv(mode: string) {
    let redirectedMode = mode;
    if (DEMO_MODES.exec(mode)) redirectedMode = "demo";
    else if (TRIALFRONTEND_MODES.exec(mode)) redirectedMode = "trialfrontend";

    return loadEnv(redirectedMode, process.cwd());
}

// https://vitejs.dev/config/
export default defineConfig(async ({ mode }) => {
    const env = loadRedirectedEnv(mode);
    const isCsp = mode === "csp";
    const port = getPort(mode);

    return {
        define: {
            "import.meta.env.VITE_BACKEND_URL": JSON.stringify(
                env.VITE_BACKEND_URL ?? createBackendUrl(mode),
            ),
        },
        optimizeDeps: {
            include: ["react/jsx-runtime"],
        },
        plugins: [
            pnpUswdsPackages(),
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
            port,
            open: true,
            // Proxy localhost/api to local prime-router
            proxy: {
                "/api": {
                    target: "http://127.0.0.1:7071",
                    changeOrigin: true,
                },
            },
            headers: {
                "content-security-policy": isCsp
                    ? `default-src 'self'; script-src 'self' https://reportstream.oktapreview.com https://global.oktacdn.com https://www.google-analytics.com https://*.in.applicationinsights.azure.com https://dap.digitalgov.gov; style-src 'self' 'unsafe-inline' https://global.oktacdn.com https://cdnjs.cloudflare.com; frame-src 'self' https://reportstream.oktapreview.com; img-src 'self' https://reportstream.oktapreview.com https://localhost:${port} data: ;connect-src 'self' https://www.google-analytics.com https://*.in.applicationinsights.azure.com https://reportstream.oktapreview.com http://localhost:${port}/api/ https://dap.digitalgov.gov;`
                    : "",
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
            devSourcemap: true,
        },
        test: {
            globals: true,
            environment: "jsdom",
            setupFiles: "./src/setupTests.ts",
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
            clearMocks: true, // TODO: re-evalulate this setting
        },
    };
});
