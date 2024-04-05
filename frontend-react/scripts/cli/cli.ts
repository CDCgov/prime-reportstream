import { Command } from "commander";
import { preview, build } from "vite";
import {
    getChildArgs,
    loadRedirectedEnv,
    frontendSpawnSync,
    getFrontendAbsolutePath,
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
program.option("--ci", "run in CI mode");

const devCmd = program
    .command("dev")
    .description(
        "run local dev server (pass vite-specific commands after '--')",
    );
devCmd.action((_, cmd: Command) => {
    process.env.NODE_ENV = "development";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    frontendSpawnSync(
        "vite",
        ["dev", "--port", "3000", ...getChildArgs(process.argv)],
        {
            env,
        },
    );
});

const buildCmd = program
    .command("build")
    .description("build app (pass vite-specific commands after '--')");
buildCmd.action((_, cmd: Command) => {
    process.env.NODE_ENV = "production";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    frontendSpawnSync(
        "vite",
        ["build", "--mode", env.VITE_MODE, ...getChildArgs(process.argv)],
        {
            env,
        },
    );
});

const testCmd = program
    .command("test")
    .description("run unit tests (pass vitest-specific commands after '--')");
testCmd.action((_, cmd: Command) => {
    process.env.NODE_ENV = "test";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    const args = [
        `--mode ${env.VITE_MODE}`,
        ...(env.CI ? ["--coverage"] : []),
        ...(opts.debug ? ["--run", "--no-file-parallelism"] : []),
        ...getChildArgs(process.argv),
    ];
    frontendSpawnSync("vitest", args, {
        env,
    });
});

const e2eCmd = program
    .command("e2e")
    .description(
        "run e2e tests (pass playwright-specific commands after '--')",
    );
e2eCmd.option("-q, --skip-build", "skip building app", false);
e2eCmd.option("-o, --open", "open preview in browser", false);
e2eCmd.action(async (_, cmd: Command) => {
    process.env.NODE_ENV = "test";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    const childArgs = getChildArgs(process.argv);
    let _server;

    // set any VITE vars returned
    process.env = env;

    // go straight to playwright if using help command
    if (!childArgs.includes("--help")) {
        if (!opts.skipBuild) await build({ mode: env.VITE_MODE });
        _server = await preview({ preview: { open: opts.open } });
    }

    frontendSpawnSync("playwright", ["test", ...childArgs], {
        env,
    });
});

const lintCmd = program
    .command("lint")
    .description("run eslint, prettier, and tsc on code");
lintCmd.option("-E, --errors-only", "show errors only");
lintCmd.option("-f, --fix", "attempt to automatically fix");
lintCmd.action((_, cmd: Command) => {
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

    for (const [cmd, args] of [
        ["eslint", eslintArgs],
        ["prettier", prettierArgs],
        ["tsc", tscArgs],
    ] as [cmd: string, args: string[]][]) {
        frontendSpawnSync(cmd, args);
    }
});

const browserslistCmd = program
    .command("browserslist")
    .description("run browserslist tasks");
const browserslistUpdateCmd = browserslistCmd
    .command("update")
    .description("update browser db");
browserslistUpdateCmd.action((_, cmd: Command) => {
    frontendSpawnSync("yarn", [
        "dlx",
        "-p",
        "browserslist",
        "-p",
        "update-browserslist-db",
        "update-browserslist-db",
    ]);
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
        frontendSpawnSync("yarn", [
            "dlx",
            "-p",
            "browserslist",
            "-p",
            "update-browserslist-db",
            "update-browserslist-db",
        ]);
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
    frontendSpawnSync("./scripts/approuter-check.sh");

    frontendSpawnSync("lint-staged");

    browserslistGenerateCmd.parse();

    frontendSpawnSync("git", ["add", "yarn.lock", "./src/browsers.json"]);
});

const prepareCmd = program
    .command("prepare")
    .description("prepare the frontend environment");
prepareCmd.action((_, cmd: Command) => {
    const opts = cmd.optsWithGlobals();

    // Don't install husky on github runners
    if (!process.env.CI && !opts.ci) {
        // run husky at repo root (needs .git folder)
        console.log("Installing husky...");
        frontendSpawnSync("husky", ["frontend-react/.husky"], {
            cwd: REPO_ABS_PATH,
        });
        if (!existsSync(join(FRONTEND_ABS_PATH, ".husky/_"))) {
            // husky error output possibly doesn't have newline
            console.error("\nHusky install failed");
            exit(1);
        }

        console.log("Fixing git hooks...");
        frontendSpawnSync("git", ["config", "core.hooksPath", ".git/hooks"], {
            cwd: REPO_ABS_PATH,
        });
    } else {
        console.log("CI mode detected. Skipping husky install...");
    }

    console.log("Frontend prepare complete");
});

program.parse();
