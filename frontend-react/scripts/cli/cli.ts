import { Command, Option } from "commander";
import { preview, build } from "vite";
import {
    getChildArgs,
    loadRedirectedEnv,
    frontendSpawnSync,
    getFrontendAbsolutePath,
    frontendSpawn,
    onExit,
    checkProcsError,
} from "./utils";
import { getRegexes } from "./generateBrowserslistRegex";
import { writeFileSync, existsSync } from "node:fs";
import { join } from "node:path";
import { exit } from "node:process";

const FRONTEND_ABS_PATH = getFrontendAbsolutePath();
const REPO_ABS_PATH = join(FRONTEND_ABS_PATH, "../");
const BROWSERS_OUTPUT_PATH = join(FRONTEND_ABS_PATH, "./src/browsers.json");

const program = new Command("reportstream-frontend-cli");
program.option(
    "--csp",
    "use Content-Security-Policy headers in web server",
    false,
);
program.option(
    "-e, --env [ENVIRONMENT]",
    "which special environment to configure for (ex: demo, trialfrontend, etc.)",
);
program.option("-d, --debug", "debug mode");
program.addOption(new Option("--ci", "run in CI mode").env("CI"));
program.option(
    "-p, --passthrough-options [OPTIONS]",
    "use if aliasing a command so as not to prevent users from passing internal options (will be passed on first before user-supplied ones)",
);

const devCmd = program
    .command("dev")
    .description(
        "run local dev server (pass vite-specific commands after '--')",
    );
devCmd.action((_, cmd: Command) => {
    process.env.NODE_ENV = "development";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);

    // set any VITE vars returned
    process.env = env;

    const proc = frontendSpawnSync(
        "vite",
        [
            "dev",
            "--port",
            "3000",
            ...getChildArgs(process.argv, opts.passthroughOptions),
        ],
        {
            env,
        },
    );

    if (checkProcsError([proc])) {
        exit(1);
    }
});

const previewCmd = program
    .command("preview")
    .description(
        "run local preview server (pass vite-specific commands after '--')",
    );
previewCmd.option("-b, --build", "build app before preview");
previewCmd.option("-s, --staging-api", "use the staging api", false);
previewCmd.action(async (_, cmd: Command) => {
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);

    // set any VITE vars returned
    process.env = env;

    if (opts.build) {
        await build({ mode: env.VITE_MODE });
    }

    const proc = frontendSpawnSync(
        "vite",
        [
            "preview",
            "--mode",
            env.VITE_MODE,
            ...getChildArgs(process.argv, opts.passthroughOptions),
        ],
        {
            env,
        },
    );

    if (checkProcsError([proc])) {
        exit(1);
    }
});

const buildCmd = program
    .command("build")
    .description("build app (pass vite-specific commands after '--')");
buildCmd.action((_, cmd: Command) => {
    process.env.NODE_ENV = "production";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);

    // set any VITE vars returned
    process.env = env;

    const proc = frontendSpawnSync(
        "vite",
        [
            "build",
            "--mode",
            env.VITE_MODE,
            ...getChildArgs(process.argv, opts.passthroughOptions),
        ],
        {
            env,
        },
    );

    if (checkProcsError([proc])) {
        exit(1);
    }
});

const testCmd = program
    .command("test")
    .description("run unit tests (pass vitest-specific commands after '--')");
testCmd.action((_, cmd: Command) => {
    process.env.NODE_ENV = "test";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    const args = [
        "--mode",
        env.VITE_MODE,
        ...(env.CI ? ["--coverage"] : []),
        ...(opts.debug ? ["--run", "--no-file-parallelism"] : []),
        ...getChildArgs(process.argv, opts.passthroughOptions),
    ];

    // set any VITE vars returned
    process.env = env;

    const proc = frontendSpawnSync("vitest", args, {
        env,
    });

    if (checkProcsError([proc])) {
        exit(1);
    }
});

const e2eCmd = program
    .command("e2e")
    .description(
        "run e2e tests (pass playwright-specific commands after '--')",
    );
e2eCmd.option("-q, --skip-build", "skip building app", false);
e2eCmd.option("-o, --open", "open preview in browser", false);
e2eCmd.option("-s, --staging-api", "use the staging api", false);
e2eCmd.action(async (_, cmd: Command) => {
    process.env.NODE_ENV = "test";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    const childArgs = getChildArgs(process.argv, opts.passthroughOptions);
    let _server;

    // set any VITE vars returned
    process.env = env;

    // go straight to playwright if using help command
    if (!childArgs.includes("--help")) {
        console.log(`Using proxy url: ${env.VITE_PROXY_URL}`);
        console.log(`Using backend url: ${env.VITE_BACKEND_URL}`);
        console.log(`Using timezone: ${env.TZ}`);
        if (!opts.skipBuild) await build({ mode: env.VITE_MODE });
        _server = await preview({
            mode: env.VITE_MODE,
            preview: { open: opts.open },
        });
    }

    try {
        // Run playwright process async so it plays nicely with preview server
        await onExit(
            frontendSpawn("playwright", ["test", ...childArgs], {
                env,
            }),
        );

        // TODO: figure out why this action prevents program from exiting naturally
        exit(0);
    } catch (e: any) {
        exit(1);
    }
});

const lintCmd = program
    .command("lint")
    .description("run eslint, prettier, and tsc on code");
lintCmd.option("-E, --errors-only", "show errors only");
lintCmd.option("-f, --fix", "attempt to automatically fix");
lintCmd.action(async (_, cmd: Command) => {
    const opts = cmd.optsWithGlobals();
    const eslintArgs = [
        "**/*.{js,ts,jsx,tsx}",
        ...(opts.errorsOnly ? ["--quiet"] : []),
        ...(opts.fix ? ["--fix"] : []),
    ];
    const prettierArgs = [
        "*",
        "--check",
        "--ignore-unknown",
        ...(opts.fix ? ["--write"] : []),
    ];
    const tscArgs: string[] = [];

    const procs = [];

    for (const [cmd, args] of [
        ["eslint", eslintArgs],
        ["prettier", prettierArgs],
        ["tsc", tscArgs],
    ] as [cmd: string, args: string[]][]) {
        procs.push(frontendSpawnSync(cmd, args));
    }

    if (checkProcsError(procs)) {
        exit(1);
    }
});

const browserslistCmd = program
    .command("browserslist")
    .description("run browserslist tasks");
const browserslistUpdateCmd = browserslistCmd
    .command("update")
    .description("update browser db");
browserslistUpdateCmd.action((_, cmd: Command) => {
    const proc = frontendSpawnSync("yarn", [
        "dlx",
        "-p",
        "browserslist",
        "-p",
        "update-browserslist-db",
        "update-browserslist-db",
    ]);
    if (checkProcsError([proc])) {
        exit(1);
    }
});
const browserslistGenerateCmd = browserslistCmd
    .command("generate")
    .description("generate project browser json");
browserslistGenerateCmd.option(
    "-D, --dry-run",
    "print the output instead of saving to file",
);
browserslistGenerateCmd.option("-q, --skip-update", "skip updating browser db");
browserslistGenerateCmd.action((_, cmd: Command) => {
    const opts = cmd.optsWithGlobals();

    // update browser db first
    if (!opts.skipUpdate) {
        const proc = frontendSpawnSync("yarn", [
            "dlx",
            "-p",
            "browserslist",
            "-p",
            "update-browserslist-db",
            "update-browserslist-db",
        ]);

        if (checkProcsError([proc])) exit(1);
    }

    const defaultOptions = {
        ignorePatch: true,
        allowHigherVersions: true,
    };
    const [prefUseragent, prefAzure] = getRegexes(defaultOptions);
    const [minUseragent, minAzure] = getRegexes({
        ...defaultOptions,
        env: "vite",
    });

    const output = {
        preferred: {
            useragent: prefUseragent.source,
            azure: prefAzure.source,
        },
        minimum: {
            useragent: minUseragent.source,
            azure: minAzure.source,
        },
    };
    const fileStr = JSON.stringify(output, undefined, 2);

    if (!opts.dryRun) {
        writeFileSync(BROWSERS_OUTPUT_PATH, fileStr);

        console.log(`Browser regexes saved to: ${BROWSERS_OUTPUT_PATH}\n`);
    } else {
        console.log("dry run");
        console.log(`${BROWSERS_OUTPUT_PATH} =>`);
        console.log(fileStr);
    }
});

const gitCmd = program.command("git").description("git tasks");
const gitHooksCmd = gitCmd
    .command("hooks")
    .description("performs tasks for particular git lifecycle steps");
const gitPrecommitHookCmd = gitHooksCmd.command("pre-commit");
gitPrecommitHookCmd.action(() => {
    const procs = [];

    procs.push(
        frontendSpawnSync("./scripts/approuter-check.sh"),

        frontendSpawnSync("lint-staged"),
        frontendSpawnSync("cli", ["browserslist", "generate"]),

        frontendSpawnSync("git", ["add", "yarn.lock", "./src/browsers.json"]),
    );

    if (checkProcsError(procs)) {
        exit(1);
    }
});

const prepareCmd = program
    .command("prepare")
    .description("prepare the frontend environment");
prepareCmd.action((_, cmd: Command) => {
    const opts = cmd.optsWithGlobals();

    // Don't install husky on github runners
    if (!process.env.CI && !opts.ci) {
        const procs = [];

        // run husky at repo root (needs .git folder)
        console.log("Installing husky...");
        procs.push(
            frontendSpawnSync("husky", ["frontend-react/.husky"], {
                cwd: REPO_ABS_PATH,
            }),
        );
        if (!existsSync(join(FRONTEND_ABS_PATH, ".husky/_"))) {
            // husky error output possibly doesn't have newline
            console.error("\nHusky install failed");
            exit(1);
        }

        console.log("Fixing git hooks...");
        procs.push(
            frontendSpawnSync(
                "git",
                ["config", "core.hooksPath", ".git/hooks"],
                {
                    cwd: REPO_ABS_PATH,
                },
            ),
        );

        if (checkProcsError(procs)) {
            exit(1);
        }
    } else {
        console.log("CI mode detected. Skipping husky install...");
    }

    console.log("Frontend prepare complete");
});

await program.parseAsync();
