import { Command } from "commander";
import { exit } from "process";
import {
    loadRedirectedEnv,
    frontendSpawnSync,
    getChildArgs,
    checkProcsError,
} from "../utils";

export const devCmd = new Command("dev")
    .description(
        "run local dev server (pass vite-specific commands after '--')",
    )
    .action((_, cmd: Command) => {
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
