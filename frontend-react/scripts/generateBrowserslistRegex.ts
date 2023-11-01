import { env, stdout } from "process";
import { writeFileSync } from "fs";
import path from "path";
import * as url from "url";

import { getUserAgentRegex } from "browserslist-useragent-regexp";

const __dirname = url.fileURLToPath(new URL(".", import.meta.url));
const OUTPUT_PATH = path.join(__dirname, "../src/utils/SupportedBrowsers.ts");

function run() {
    const preferredBrowsersRegex = getUserAgentRegex({
        allowHigherVersions: true,
        ignorePatch: true,
    });
    env.BROWSERSLIST_ENV = "vite";
    const minimumBrowsersRegex = getUserAgentRegex({
        ignorePatch: true,
        allowHigherVersions: true,
    });

    writeFileSync(
        OUTPUT_PATH,
        `export const preferredBrowsersRegex =\n    ${preferredBrowsersRegex};\nexport const minimumBrowsersRegex =\n    ${minimumBrowsersRegex};\n`,
    );

    stdout.write(`Browser regexes saved to: ${OUTPUT_PATH}\n`);
}

run();
