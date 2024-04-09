import { Command } from "commander";
import { exit } from "process";
import {
    loadRedirectedEnv,
    frontendSpawnSync,
    getChildArgs,
    checkProcsError,
} from "../utils";

export const buildCmd = new Command("build")
    .description("build app (pass vite-specific commands after '--')")
    .action((_, cmd: Command) => {
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
