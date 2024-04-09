import { Command } from "commander";
import { writeFileSync } from "fs";
import { exit } from "process";
import { getRegexes } from "../generateBrowserslistRegex";
import {
    frontendSpawnSync,
    checkProcsError,
    BROWSERS_OUTPUT_PATH,
} from "../utils";

export const browserslistCmd = new Command("browserslist").description(
    "run browserslist tasks",
);

const browserslistUpdateCmd = browserslistCmd
    .command("update")
    .description("update browser db")
    .action((_, cmd: Command) => {
        const proc = frontendSpawnSync("yarn", [
            "dlx",
            "-p",
            "browserslist",
            "-p",
            "update-browserslist-db",
            "update-browserslist-db",
        ]);
        if (checkProcsError([proc])) {
            exit(1);
        }
    });

const browserslistGenerateCmd = browserslistCmd
    .command("generate")
    .description("generate project browser json")
    .option("-D, --dry-run", "print the output instead of saving to file")
    .option("-q, --skip-update", "skip updating browser db")
    .action((_, cmd: Command) => {
        const opts = cmd.optsWithGlobals();

        // update browser db first
        if (!opts.skipUpdate) {
            const proc = frontendSpawnSync("yarn", [
                "dlx",
                "-p",
                "browserslist",
                "-p",
                "update-browserslist-db",
                "update-browserslist-db",
            ]);

            if (checkProcsError([proc])) exit(1);
        }

        const defaultOptions = {
            ignorePatch: true,
            allowHigherVersions: true,
        };
        const [prefUseragent, prefAzure] = getRegexes(defaultOptions);
        const [minUseragent, minAzure] = getRegexes({
            ...defaultOptions,
            env: "vite",
        });

        const output = {
            preferred: {
                useragent: prefUseragent.source,
                azure: prefAzure.source,
            },
            minimum: {
                useragent: minUseragent.source,
                azure: minAzure.source,
            },
        };
        const fileStr = JSON.stringify(output, undefined, 2);

        if (!opts.dryRun) {
            writeFileSync(BROWSERS_OUTPUT_PATH, fileStr);

            console.log(`Browser regexes saved to: ${BROWSERS_OUTPUT_PATH}\n`);
        } else {
            console.log("dry run");
            console.log(`${BROWSERS_OUTPUT_PATH} =>`);
            console.log(fileStr);
        }
    });
