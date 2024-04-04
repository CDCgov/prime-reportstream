import { loadEnv } from "vite";
const DEMO_MODES = /^demo\d+$/;
const TRIALFRONTEND_MODES = /^trialfrontend\d+$/;

export function getApiProxyUrl(NODE_ENV: string, RS_ENV?: string) {
    // local
    if (NODE_ENV === "development") return "http://127.0.0.1:7071";

    // rs env
    const subdomain = NODE_ENV === "test" ? "staging" : RS_ENV;
    return `https://${subdomain ? subdomain + "." : ""}prime.cdc.gov`;
}

export function getBackendUrl(NODE_ENV: string, RS_ENV?: string) {
    // local
    if (NODE_ENV !== "production") {
        let url = "http://localhost";

        if (NODE_ENV === "development") {
            url += ":3000";
        } else {
            // preview server
            url += ":4173";
        }

        return url;
    }

    // rs env
    const subdomain = RS_ENV?.startsWith("trialfrontend") ? "staging" : RS_ENV;
    return `https://${subdomain ? subdomain + "." : ""}prime.cdc.gov`;
}

/**
 * Simplify known number-ranged rs envs to base env (ex: trialfrontend01 -> trialfrontend)
 */
export function loadRedirectedEnv(
    { env, csp, debug, ci }: any,
    dir = process.cwd(),
) {
    let redirectedEnv = process.env.NODE_ENV ?? "production";
    if (DEMO_MODES.exec(env)) redirectedEnv = "demo";
    else if (TRIALFRONTEND_MODES.exec(env)) redirectedEnv = "trialfrontend";
    const loadedEnv = loadEnv(redirectedEnv, dir, "");

    const envVars = {
        CI: ci ? "true" : "",
        DEBUG_PRINT_LIMIT: debug ? "100000" : "",
        VITE_MODE: redirectedEnv,
        VITE_BACKEND_URL: getBackendUrl(loadedEnv.NODE_ENV),
        VITE_PROXY_URL: getApiProxyUrl(loadedEnv.NODE_ENV, env),
        VITE_CSP: csp ? createContentSecurityPolicy() : "",
        TZ: loadedEnv.NODE_ENV === "test" ? "UTC" : "",
        ...process.env,
        ...loadedEnv,
    };

    return envVars;
}

export function createContentSecurityPolicy() {
    return `default-src 'self'; script-src 'self' https://reportstream.oktapreview.com https://global.oktacdn.com https://www.google-analytics.com https://*.in.applicationinsights.azure.com https://dap.digitalgov.gov; style-src 'self' 'unsafe-inline' https://global.oktacdn.com https://cdnjs.cloudflare.com; frame-src 'self' https://reportstream.oktapreview.com; img-src 'self' https://reportstream.oktapreview.com data: ;connect-src 'self' https://www.google-analytics.com https://*.in.applicationinsights.azure.com https://reportstream.oktapreview.com https://dap.digitalgov.gov;`;
}

export function getChildArgs(args: string[]) {
    const separatorIndex = args.indexOf("--");
    if (separatorIndex > 0) {
        return args.slice(separatorIndex + 1);
    }

    return [];
}
