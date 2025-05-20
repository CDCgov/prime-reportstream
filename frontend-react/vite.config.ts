/// <reference types="vitest" />

import { resolve } from "path";
import { defineConfig, loadEnv, ConfigEnv, UserConfig } from "vite";
import type { CoverageIstanbulOptions, CoverageV8Options, CustomProviderOptions } from "vitest";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";
import mdx from "@mdx-js/rollup";
import rehypeSlug from "rehype-slug";
import { remarkMdxToc } from "remark-mdx-toc";
import remarkFrontmatter from "remark-frontmatter";
import remarkMdxFrontmatter from "remark-mdx-frontmatter";
import { checker } from "vite-plugin-checker";
import type { Pluggable } from "unified";

const LOCAL_BACKEND_MODES = ["preview", "development", "test", "csp", "ci"];
const LOCAL_PROXY_MODES = ["preview", "development", "test", "csp"];
const DEMO_MODES = /^demo\d+$/;
const TRIALFRONTEND_MODES = /^trialfrontend\d+$/;

function createProxyUrl(mode: string) {
    if (LOCAL_PROXY_MODES.includes(mode)) return "http://127.0.0.1:7071";
    const subdomain = mode === "production" ? "" : mode === "ci" ? "staging" : mode;
    return `https://${subdomain ? subdomain + "." : ""}prime.cdc.gov`;
}

function createBackendUrl(mode: string) {
    const port = getPort(mode);
    if (LOCAL_BACKEND_MODES.includes(mode)) return `http://localhost${port ? ":" + port.toString() : ""}`;
    return `https://${mode !== "production" ? (mode.startsWith("trialfrontend") ? "staging" : mode) + "." : ""}prime.cdc.gov`;
}

function getPort(mode: string) {
    switch (mode) {
        case "ci":
        case "test":
        case "csp":
        case "preview":
            return 4173;
        default:
            return 3000;
    }
}

function loadRedirectedEnv(mode: string) {
    let redirectedMode = mode;
    if (mode === "ci") redirectedMode = "test";
    if (DEMO_MODES.exec(mode)) redirectedMode = "demo";
    else if (TRIALFRONTEND_MODES.exec(mode)) redirectedMode = "trialfrontend";
    return loadEnv(redirectedMode, process.cwd());
}

interface ExtendedUserConfig extends UserConfig {
    css?: {
        preprocessorOptions?: {
            scss?: {
                loadPaths?: string[];
            };
        };
        devSourcemap?: boolean;
    };
    test?: {
        globals?: boolean;
        environment?: string;
        setupFiles?: string;
        globalSetup?: string;
        include?: string[];
        css?: boolean;
        coverage?:
            | ({ provider: "istanbul" } & CoverageIstanbulOptions)
            | ({ provider: "v8" } & CoverageV8Options)
            | ({ provider: "custom" } & CustomProviderOptions);
        clearMocks?: boolean;
        server?: {
            deps?: {
                inline?: string[];
            };
        };
    };
}

const createConfig = async ({ mode }: ConfigEnv): Promise<ExtendedUserConfig> => {
    const env = loadRedirectedEnv(mode);
    const backendUrl = env.VITE_BACKEND_URL ?? createBackendUrl(mode);
    const proxyUrl = env.PROXY_URL ?? createProxyUrl(mode);
    const port = getPort(mode);
    const isCsp = mode === "csp";
    const disableOverlays = !!process.env.DISABLE_OVERLAYS;

    return {
        define: {
            "import.meta.env.VITE_BACKEND_URL": JSON.stringify(backendUrl),
        },
        optimizeDeps: {
            include: ["react/jsx-runtime"],
        },
        plugins: [
            react(),
            mdx({
                mdExtensions: [],
                providerImportSource: "@mdx-js/react",
                remarkPlugins: [remarkMdxToc as Pluggable, remarkFrontmatter, remarkMdxFrontmatter],
                rehypePlugins: [rehypeSlug as Pluggable],
            }),
            svgr(),
            checker({
                overlay: !disableOverlays,
                typescript: true,
                eslint: {
                    lintCommand: 'eslint "./src/**/*[!.test][!.stories].{ts,tsx}"',
                    useFlatConfig: true,
                },
            }),
        ],
        server: {
            port,
            open: true,
            proxy: {
                "/api": {
                    target: proxyUrl,
                    changeOrigin: true,
                },
            },
            headers: {
                "content-security-policy": isCsp
                    ? "default-src 'self';" +
                      " script-src 'self'" +
                      " https://reportstream.oktapreview.com" +
                      " https://global.oktacdn.com" +
                      " https://www.google-analytics.com" +
                      " https://*.in.applicationinsights.azure.com" +
                      " https://dap.digitalgov.gov" +
                      " https://www.googletagmanager.com;" +
                      " style-src 'self' 'unsafe-inline'" +
                      " https://global.oktacdn.com" +
                      " https://cdnjs.cloudflare.com;" +
                      " frame-src 'self'" +
                      " https://reportstream.oktapreview.com;" +
                      " img-src 'self'" +
                      " https://reportstream.oktapreview.com" +
                      ` https://localhost:${port}` +
                      " data:;" +
                      " connect-src 'self'" +
                      " https://www.google-analytics.com" +
                      " https://*.in.applicationinsights.azure.com" +
                      " https://reportstream.oktapreview.com" +
                      ` http://localhost:${port}/api/` +
                      " https://dap.digitalgov.gov" +
                      " https://www.googletagmanager.com" +
                      " https://js.monitor.azure.com/"
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
                    unsupportedBrowser: resolve(__dirname, "unsupported-browser.html"),
                },
            },
        },
        css: {
            preprocessorOptions: {
                scss: {
                    loadPaths: ["node_modules/@uswds/uswds/packages"],
                },
            },
            devSourcemap: true,
        },
        test: {
            globals: true,
            environment: "jsdom",
            setupFiles: "./src/setupTests.ts",
            globalSetup: "./src/globalSetup.ts",
            include: ["./src/**/__tests__/**/*.[jt]s?(x)", "./src/**/?(*.)+(spec|test).[jt]s?(x)"],
            css: false,
            coverage: {
                include: ["src/**/*.{js,jsx,ts,tsx}", "!src/**/*.d.ts"],
                provider: "istanbul",
                all: false,
                reporter: ["clover", "json", "lcov", "text"],
            },
            clearMocks: true, // TODO: revisit this setting,
            server: {
                deps: {
                    inline: ["@trussworks/react-uswds"],
                },
            },
        },
        resolve: {
            alias: {
                "msw/native": resolve(__dirname, "./node_modules/msw/lib/native/index.mjs"),
            },
        },
    };
};

export default defineConfig((configEnv) => createConfig(configEnv));
