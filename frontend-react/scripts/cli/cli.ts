import { Command, Option } from "commander";
import { browserslistCmd } from "./commands/browserslist";
import { buildCmd } from "./commands/build";
import { devCmd } from "./commands/dev";
import { e2eCmd } from "./commands/e2e";
import { gitCmd } from "./commands/git";
import { lintCmd } from "./commands/lint";
import { prepareCmd } from "./commands/prepare";
import { previewCmd } from "./commands/preview";
import { testCmd } from "./commands/test";

const program = new Command("reportstream-frontend-cli")
    .option("--csp", "use Content-Security-Policy headers in web server", false)
    .option(
        "-e, --env [ENVIRONMENT]",
        "which special environment to configure for (ex: demo, trialfrontend, etc.)",
    )
    .option("-d, --debug", "debug mode")
    .addOption(new Option("--ci", "run in CI mode").env("CI"))
    .option(
        "-p, --passthrough-options [OPTIONS]",
        "use if aliasing a command so as not to prevent users from passing internal options (will be passed on first before user-supplied ones)",
    );

const commands = [
    browserslistCmd,
    buildCmd,
    devCmd,
    e2eCmd,
    gitCmd,
    lintCmd,
    prepareCmd,
    previewCmd,
    testCmd,
];

for (const cmd of commands) {
    program.addCommand(cmd);
}

await program.parseAsync();
