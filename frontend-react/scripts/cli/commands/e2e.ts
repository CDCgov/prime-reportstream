import { Command } from "commander";
import { exit } from "process";
import { build, preview } from "vite";
import {
    loadRedirectedEnv,
    getChildArgs,
    onExit,
    frontendSpawn,
} from "../utils";

export const e2eCmd = new Command("e2e")
    .description("run e2e tests (pass playwright-specific commands after '--')")
    .option("-q, --skip-build", "skip building app", false)
    .option("-o, --open", "open preview in browser", false)
    .option("-s, --staging-api", "use the staging api", false)
    .action(async (_, cmd: Command) => {
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
