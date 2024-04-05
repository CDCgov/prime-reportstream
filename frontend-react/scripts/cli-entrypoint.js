#!/usr/bin/env node

import { spawnSync } from "node:child_process";

// run in child process to call tsx
const proc = spawnSync(
    "yarn",
    ["run", "tsx", "./scripts/cli/cli.ts", ...process.argv.slice(2)],
    { stdio: "inherit" },
);

process.exit(proc.status ? 1 : 0);
