import { Command } from "commander";
import { preview, build } from "vite";
import { getChildArgs, loadRedirectedEnv } from "./utils";
import { spawnSync } from "node:child_process";
import { exit } from "node:process";
import { OUTPUT_PATH, getRegexes } from "./generateBrowserslistRegex";
import { writeFileSync } from "node:fs";

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
    spawnSync("vite", ["dev", ...getChildArgs(process.argv)], {
        env,
        stdio: "inherit",
    });
});

const buildCmd = program
    .command("build")
    .description("build app (pass vite-specific commands after '--')");
buildCmd.action((_, cmd: Command) => {
    process.env.NODE_ENV = "production";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    spawnSync(
        "vite",
        ["build", `--mode ${env.VITE_MODE}`, ...getChildArgs(process.argv)],
        {
            env,
            stdio: "inherit",
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
    spawnSync("vitest", args, {
        env,
        stdio: "inherit",
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

    spawnSync("playwright", ["test", ...childArgs], {
        env,
        stdio: "inherit",
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
        spawnSync(cmd, args, { stdio: "inherit" });
    }
});

const browserslistCmd = program
    .command("browserslist")
    .description("run browserslist tasks");
const browserslistUpdateCmd = browserslistCmd
    .command("update")
    .description("update browser db");
browserslistUpdateCmd.action((_, cmd: Command) => {
    spawnSync(
        "yarn",
        [
            "dlx",
            "-p",
            "browserslist",
            "-p",
            "update-browserslist-db",
            "update-browserslist-db",
        ],
        { stdio: "inherit" },
    );
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
        spawnSync(
            "yarn",
            [
                "dlx",
                "-p",
                "browserslist",
                "-p",
                "update-browserslist-db",
                "update-browserslist-db",
            ],
            { stdio: "inherit" },
        );
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
        writeFileSync(OUTPUT_PATH, fileStr);

        console.log(`Browser regexes saved to: ${OUTPUT_PATH}\n`);
    } else {
        console.log("dry run");
        console.log(`${OUTPUT_PATH} =>`);
        console.log(fileStr);
    }
});

program.parse();
