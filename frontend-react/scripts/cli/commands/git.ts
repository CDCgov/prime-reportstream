import { Command } from "commander";
import { exit } from "process";
import { frontendSpawnSync, checkProcsError } from "../utils";

export const gitCmd = new Command("git").description("git tasks");

const gitHooksCmd = gitCmd
    .command("hooks")
    .description("performs tasks for particular git lifecycle steps");

const gitPrecommitHookCmd = gitHooksCmd.command("pre-commit").action(() => {
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
