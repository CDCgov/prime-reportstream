import { Command } from "commander";
import { exit } from "process";
import { frontendSpawnSync, checkProcsError } from "../utils";

export const lintCmd = new Command("lint")
    .description("run eslint, prettier, and tsc on code")
    .option("-E, --errors-only", "show errors only")
    .option("-f, --fix", "attempt to automatically fix")
    .action(async (_, cmd: Command) => {
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

        const procs = [];

        for (const [cmd, args] of [
            ["eslint", eslintArgs],
            ["prettier", prettierArgs],
            ["tsc", tscArgs],
        ] as [cmd: string, args: string[]][]) {
            procs.push(frontendSpawnSync(cmd, args));
        }

        if (checkProcsError(procs)) {
            exit(1);
        }
    });
