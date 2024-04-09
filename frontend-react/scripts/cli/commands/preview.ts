import { Command } from "commander";
import { exit } from "process";
import { build } from "vite";
import {
    loadRedirectedEnv,
    frontendSpawnSync,
    getChildArgs,
    checkProcsError,
} from "../utils";

export const previewCmd = new Command("preview")
    .description(
        "run local preview server (pass vite-specific commands after '--')",
    )
    .option("-b, --build", "build app before preview")
    .option("-s, --staging-api", "use the staging api", false)
    .action(async (_, cmd: Command) => {
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
