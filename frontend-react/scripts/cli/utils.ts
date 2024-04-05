import {
    ChildProcess,
    ChildProcessByStdio,
    ChildProcessWithoutNullStreams,
    SpawnOptions,
    SpawnOptionsWithStdioTuple,
    SpawnOptionsWithoutStdio,
    SpawnSyncOptions,
    SpawnSyncOptionsWithBufferEncoding,
    SpawnSyncOptionsWithStringEncoding,
    SpawnSyncReturns,
    StdioNull,
    StdioPipe,
    spawn,
    spawnSync,
} from "node:child_process";
import { loadEnv } from "vite";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { Writable, Readable } from "node:stream";

const DEMO_MODES = /^demo\d+$/;
const TRIALFRONTEND_MODES = /^trialfrontend\d+$/;
const __dirname = getUrlDirname(import.meta.url);

export function getApiProxyUrl(
    NODE_ENV: string,
    RS_ENV?: string,
    isStagingApi = false,
) {
    // local
    if (NODE_ENV === "development" || (NODE_ENV === "test" && !isStagingApi))
        return "http://127.0.0.1:7071";

    // rs env
    const subdomain = NODE_ENV === "test" ? "staging" : RS_ENV;
    return `https://${subdomain ? subdomain + "." : ""}prime.cdc.gov`;
}

export function getBackendUrl(
    NODE_ENV: string,
    RS_ENV?: string,
    isProxyStagingApi = false,
) {
    // local
    if (NODE_ENV !== "production" || isProxyStagingApi) {
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
    { env, csp, debug, ci, stagingApi }: any,
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
        VITE_BACKEND_URL: getBackendUrl(
            loadedEnv.NODE_ENV,
            env,
            stagingApi || ci,
        ),
        VITE_PROXY_URL: getApiProxyUrl(
            loadedEnv.NODE_ENV,
            env,
            stagingApi || ci,
        ),
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

export function getChildArgs(args: string[], passthroughOptions?: string) {
    const childArgs = [];

    if (passthroughOptions) {
        const pOpts = passthroughOptions.split(" ");
        if (pOpts[0]?.startsWith("=")) {
            pOpts[0] = pOpts[0].slice(1);
        }
        childArgs.push(...pOpts);
    }

    const separatorIndex = args.indexOf("--");
    if (separatorIndex > 0) {
        childArgs.push(...args.slice(separatorIndex + 1));
    }

    return childArgs;
}

export function getUrlDirname(url: string) {
    return fileURLToPath(new URL(".", url));
}

export function getFrontendAbsolutePath() {
    return join(__dirname, "../../");
}

export function frontendSpawnSync(command: string): SpawnSyncReturns<Buffer>;
export function frontendSpawnSync(
    command: string,
    options: SpawnSyncOptionsWithStringEncoding,
): SpawnSyncReturns<string>;
export function frontendSpawnSync(
    command: string,
    options: SpawnSyncOptionsWithBufferEncoding,
): SpawnSyncReturns<Buffer>;
export function frontendSpawnSync(
    command: string,
    options?: SpawnSyncOptions,
): SpawnSyncReturns<string | Buffer>;
export function frontendSpawnSync(
    command: string,
    args: readonly string[],
): SpawnSyncReturns<Buffer>;
export function frontendSpawnSync(
    command: string,
    args: readonly string[],
    options: SpawnSyncOptionsWithStringEncoding,
): SpawnSyncReturns<string>;
export function frontendSpawnSync(
    command: string,
    args: readonly string[],
    options: SpawnSyncOptionsWithBufferEncoding,
): SpawnSyncReturns<Buffer>;
export function frontendSpawnSync(
    command: string,
    args?: readonly string[],
    options?: SpawnSyncOptions,
): SpawnSyncReturns<string | Buffer>;
export function frontendSpawnSync(
    command: string,
    optionsOrArgs?:
        | readonly string[]
        | SpawnSyncOptionsWithStringEncoding
        | SpawnSyncOptionsWithBufferEncoding
        | SpawnSyncOptions,
    options?:
        | SpawnSyncOptionsWithStringEncoding
        | SpawnSyncOptionsWithBufferEncoding
        | SpawnSyncOptions,
) {
    let opts:
        | SpawnSyncOptionsWithStringEncoding
        | SpawnSyncOptionsWithBufferEncoding
        | SpawnSyncOptions
        | undefined;
    let args: readonly string[] | undefined;

    if (Array.isArray(optionsOrArgs)) {
        args = optionsOrArgs;
        opts = options;
    } else {
        opts = optionsOrArgs as
            | SpawnSyncOptionsWithStringEncoding
            | SpawnSyncOptionsWithBufferEncoding
            | SpawnSyncOptions;
    }

    // set cwd to frontend-react dir
    opts = {
        cwd: getFrontendAbsolutePath(),
        stdio: "inherit",
        ...opts,
    };

    return spawnSync(command, args, opts);
}

export function frontendSpawn(
    command: string,
    options?: SpawnOptionsWithoutStdio,
): ChildProcessWithoutNullStreams;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioPipe, StdioPipe>,
): ChildProcessByStdio<Writable, Readable, Readable>;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioPipe, StdioNull>,
): ChildProcessByStdio<Writable, Readable, null>;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioNull, StdioPipe>,
): ChildProcessByStdio<Writable, null, Readable>;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioPipe, StdioPipe>,
): ChildProcessByStdio<null, Readable, Readable>;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioNull, StdioNull>,
): ChildProcessByStdio<Writable, null, null>;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioPipe, StdioNull>,
): ChildProcessByStdio<null, Readable, null>;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioNull, StdioPipe>,
): ChildProcessByStdio<null, null, Readable>;
export function frontendSpawn(
    command: string,
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioNull, StdioNull>,
): ChildProcessByStdio<null, null, null>;
export function frontendSpawn(
    command: string,
    options: SpawnOptions,
): ChildProcess;
// overloads of spawn with 'args'
export function frontendSpawn(
    command: string,
    args?: readonly string[],
    options?: SpawnOptionsWithoutStdio,
): ChildProcessWithoutNullStreams;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioPipe, StdioPipe>,
): ChildProcessByStdio<Writable, Readable, Readable>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioPipe, StdioNull>,
): ChildProcessByStdio<Writable, Readable, null>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioNull, StdioPipe>,
): ChildProcessByStdio<Writable, null, Readable>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioPipe, StdioPipe>,
): ChildProcessByStdio<null, Readable, Readable>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioPipe, StdioNull, StdioNull>,
): ChildProcessByStdio<Writable, null, null>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioPipe, StdioNull>,
): ChildProcessByStdio<null, Readable, null>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioNull, StdioPipe>,
): ChildProcessByStdio<null, null, Readable>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptionsWithStdioTuple<StdioNull, StdioNull, StdioNull>,
): ChildProcessByStdio<null, null, null>;
export function frontendSpawn(
    command: string,
    args: readonly string[],
    options: SpawnOptions,
): ChildProcess;
export function frontendSpawn(
    command: any,
    optionsOrArgs?: any,
    options?: any,
) {
    let opts:
        | SpawnOptionsWithStdioTuple<any, any, any>
        | SpawnOptionsWithoutStdio
        | SpawnOptions
        | undefined;
    let args: readonly string[] | undefined;

    if (Array.isArray(optionsOrArgs)) {
        args = optionsOrArgs;
        opts = options;
    } else {
        opts = optionsOrArgs as
            | SpawnOptionsWithStdioTuple<any, any, any>
            | SpawnOptionsWithoutStdio
            | SpawnOptions;
    }

    // set cwd to frontend-react dir
    opts = {
        cwd: getFrontendAbsolutePath(),
        stdio: "inherit",
        ...(opts ?? {}),
    };

    return spawn(command, args as any, opts as any) as any;
}

export function createPromiseResolvers<T = unknown>() {
    let resolve, reject;
    const promise = new Promise<T>((res, rej) => {
        resolve = res;
        reject = rej;
    });
    return { promise, resolve, reject };
}
