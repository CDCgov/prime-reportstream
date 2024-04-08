import { Command } from "commander";
import { exit } from "process";
import {
    loadRedirectedEnv,
    getChildArgs,
    frontendSpawnSync,
    checkProcsError,
} from "../utils";

export const testCmd = new Command("test")
    .description("run unit tests (pass vitest-specific commands after '--')")
    .action((_, cmd: Command) => {
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
