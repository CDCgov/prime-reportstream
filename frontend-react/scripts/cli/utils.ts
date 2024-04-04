import { Command } from "commander";
import { loadEnv } from "vite";

const LOCAL_BACKEND_MODES = ["preview", "development", "test", "csp", "ci"];
const LOCAL_PROXY_MODES = ["preview", "development", "test", "csp"];
const DEMO_MODES = /^demo\d+$/;
const TRIALFRONTEND_MODES = /^trialfrontend\d+$/;

/**
 * Determine the backend url based on mode.
 */
export function createProxyUrl(mode: string) {
    if (LOCAL_PROXY_MODES.includes(mode)) return "http://127.0.0.1:7071";
    const subdomain =
        mode === "production" ? "" : mode === "ci" ? "staging" : mode;
    return `https://${subdomain ? subdomain + "." : ""}prime.cdc.gov`;
}

export function createBackendUrl(mode: string) {
    if (process.env.CI) return "http://localhost";

    const port = getPort(mode);
    if (LOCAL_BACKEND_MODES.includes(mode))
        return `http://localhost${port ? ":" + port.toString() : ""}`;
    return `https://${mode !== "production" ? (mode.startsWith("trialfrontend") ? "staging" : mode) + "." : ""}prime.cdc.gov`;
}

export function getPort(mode: string) {
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

/**
 * Simplify known number-ranged modes to base mode (ex: trialfrontend01 -> trialfrontend)
 */
export function loadRedirectedEnv(
    { env: mode, csp, debug }: any,
    dir = process.cwd(),
) {
    let redirectedMode = mode;
    if (mode === "ci") redirectedMode = "test";
    if (DEMO_MODES.exec(mode)) redirectedMode = "demo";
    else if (TRIALFRONTEND_MODES.exec(mode)) redirectedMode = "trialfrontend";
    const loadedEnv = loadEnv(redirectedMode, dir, "");

    const port = getPort(mode).toString();

    const env = {
        CI: mode === "ci" ? "true" : "",
        DEBUG_PRINT_LIMIT: debug ? "100000" : "",
        VITE_PORT: port,
        VITE_BACKEND_URL: createBackendUrl(mode),
        VITE_PROXY_URL: createProxyUrl(mode),
        VITE_CSP: csp ? createContentSecurityPolicy(port) : undefined,
        TZ: redirectedMode === "test" ? "UTC" : "",
        ...process.env,
        ...loadedEnv,
    };

    return env;
}

export function createContentSecurityPolicy(port: string) {
    return `default-src 'self'; script-src 'self' https://reportstream.oktapreview.com https://global.oktacdn.com https://www.google-analytics.com https://*.in.applicationinsights.azure.com https://dap.digitalgov.gov; style-src 'self' 'unsafe-inline' https://global.oktacdn.com https://cdnjs.cloudflare.com; frame-src 'self' https://reportstream.oktapreview.com; img-src 'self' https://reportstream.oktapreview.com https://localhost:${port} data: ;connect-src 'self' https://www.google-analytics.com https://*.in.applicationinsights.azure.com https://reportstream.oktapreview.com http://localhost:${port}/api/ https://dap.digitalgov.gov;`;
}

export function getChildArgs(args: string[]) {
    const separatorIndex = args.indexOf("--");
    if (separatorIndex > 0) {
        return args.slice(separatorIndex + 1);
    }

    return [];
}
