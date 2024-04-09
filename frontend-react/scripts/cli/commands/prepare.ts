import { Command } from "commander";
import { existsSync } from "node:fs";
import { exit } from "node:process";
import {
    frontendSpawnSync,
    checkProcsError,
    FRONTEND_ABS_PATH,
    REPO_ABS_PATH,
} from "../utils";
import { join } from "node:path";

export const prepareCmd = new Command("prepare")
    .description("prepare the frontend environment")
    .action((_, cmd: Command) => {
        const opts = cmd.optsWithGlobals();

        // Don't install husky on github runners
        if (!opts.ci) {
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
