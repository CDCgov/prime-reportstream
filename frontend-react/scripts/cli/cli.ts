import { Command } from "commander";
import { preview, build } from "vite";
import { getChildArgs, loadRedirectedEnv } from "./utils";
import { spawn } from "node:child_process";

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
devCmd.action(async (_, cmd: Command) => {
    process.env.NODE_ENV = "development";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    const child = spawn(
        "vite",
        ["dev", `--mode ${env.VITE_MODE}`, ...getChildArgs(process.argv)],
        {
            env,
            stdio: "inherit",
        },
    );
});

const buildCmd = program
    .command("build")
    .description("build app (pass vite-specific commands after '--')");
buildCmd.action(async (_, cmd: Command) => {
    process.env.NODE_ENV = "production";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    const child = spawn(
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
testCmd.action(async (_, cmd: Command) => {
    process.env.NODE_ENV = "test";
    const opts = cmd.optsWithGlobals();
    const env = loadRedirectedEnv(opts);
    const args = [
        `--mode ${env.VITE_MODE}`,
        ...(env.CI ? ["--coverage"] : []),
        ...(opts.debug ? ["--run", "--no-file-parallelism"] : []),
        ...getChildArgs(process.argv),
    ];
    const child = spawn("vitest", args, {
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

    // go straight to playwright if using help command
    if (childArgs.includes("--help")) {
        const child = spawn(
            "playwright",
            ["test", ...getChildArgs(process.argv)],
            {
                env,
                stdio: "inherit",
            },
        );
        return;
    }

    // set any VITE vars returned
    process.env = env;

    if (!opts.skipBuild) await build({ mode: env.VITE_MODE });
    const server = await preview({ preview: { open: opts.open } });
    const child = spawn("playwright", ["test", ...getChildArgs(process.argv)], {
        env,
        stdio: "inherit",
    });
});

program.parse();
