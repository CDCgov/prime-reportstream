#!/usr/bin/env node

import { spawnSync } from "node:child_process";

// run in child process to call tsx
spawnSync(
    "yarn",
    ["run", "tsx", "./scripts/cli/cli.ts", ...process.argv.slice(2)],
    { env: { ...process.env }, stdio: "inherit" },
);
