#!/usr/bin/env node

import { spawn } from "node:child_process";

// run in child process to call tsx
spawn(
    "yarn",
    ["run", "tsx", "./scripts/cli/cli.ts", ...process.argv.slice(2)],
    { env: { ...process.env }, stdio: "inherit" },
);
