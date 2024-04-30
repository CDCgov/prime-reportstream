/* eslint-disable camelcase */
import { writeFileSync } from "node:fs";
import path from "node:path";
import * as url from "node:url";
import process from "node:process";

import {
    getBrowsersList,
    compileRegex,
    applyVersionsToRegexes,
    UserAgentRegexOptions,
    getRegexesForBrowsers,
    mergeBrowserVersions,
    BrowserRegex,
} from "browserslist-useragent-regexp";

const __dirname = url.fileURLToPath(new URL(".", import.meta.url));
const OUTPUT_PATH = path.join(__dirname, "../src/browsers.json");

/**
 * Desktop browser names:
 *
 * Firefox
 * Chrome
 * Edge
 * Safari
 *
 * iOS browser names (not perfect because for iOS all browsers actually render using safari but
 * Azure Monitor is only going to give us the app browser info):
 *
 * Firefox iOS
 * Chrome Mobile iOS
 * Mobile Safari (Edge seemingly properly formats their user-agent string to report as Mobile Safari)
 *
 * Android browser names:
 *
 * Firefox Mobile
 * Chrome Mobile
 * Edge Mobile
 */
const azureFamilySourceRegexMap = {
    and_ff: "Firefox Mobile (\\d+)\\.(\\d+)(\\.(\\d+)|)",
    and_chr: "Chrome Mobile (\\d+)\\.(\\d+)(\\.(\\d+)|)",
    and_edge: "Edge Mobile (\\d+)\\.(\\d+)(\\.(\\d+)|)",
    ios_ff: "Firefox iOS (\\d+)\\.(\\d+)(\\.(\\d+)|)",
    ios_chr: "Chrome Mobile iOS (\\d+)\\.(\\d+)(\\.(\\d+)|)",
    ios_edge: "Mobile Safari (\\d+)[_.](\\d+)([_.](\\d+)|)",
    ios_saf: "Mobile Safari (\\d+)[_.](\\d+)([_.](\\d+)|)",
    firefox: "Firefox (\\d+).(\\d+)(\\.(\\d+)|)",
    chrome: "Chrome (\\d+)\\.(\\d+)(\\.(\\d+)|)",
    edge: "Edge (\\d+)\\.(\\d+)(\\.(\\d+)|)",
    safari: "Safari (\\d+)\\.(\\d+)([.,](\\d+)|)",
} as const;

type AzureFamily = keyof typeof azureFamilySourceRegexMap;

/**
 * Create custom BrowserRegex fitting Azure from desktop version.
 */
function getAzureBrowserRegex(
    family: AzureFamily,
    sources: BrowserRegex[],
): BrowserRegex {
    const regex = new RegExp(azureFamilySourceRegexMap[family]);
    let source = sources.find((s) => s.family === family);

    switch (family) {
        case "ios_ff":
            source = sources.find((s) => s.family === "firefox");
            break;
        case "ios_chr":
            source = sources.find((s) => s.family === "chrome");
            break;
        case "ios_edge":
        case "and_edge":
            source = sources.find((s) => s.family === "edge");
            break;
    }

    if (!source)
        throw new Error(`Could not find nearest source for: ${family}`);

    return {
        ...source,
        family,
        regex,
    };
}

/**
 * Perform from-scratch building up to getRegexesForBrowsers so that we can then
 * create an azure-form copy to then create single regexes for both.
 */
function getRegexes(
    options: UserAgentRegexOptions = {},
): [useragent: RegExp, azure: RegExp] {
    const browsersList = getBrowsersList(options);
    const mergedBrowsers = mergeBrowserVersions(browsersList);

    const sourceRegexes = getRegexesForBrowsers(mergedBrowsers, options);
    const azureSourceRegex = Object.keys(azureFamilySourceRegexMap).map((k) =>
        getAzureBrowserRegex(k as AzureFamily, sourceRegexes),
    );

    const versionedRegexes = applyVersionsToRegexes(sourceRegexes, options);
    const azureVersionedRegexes = applyVersionsToRegexes(
        azureSourceRegex,
        options,
    );

    const versionedRegex = compileRegex(versionedRegexes);
    const azureVersionedRegex = compileRegex(azureVersionedRegexes);

    return [versionedRegex, azureVersionedRegex];
}

/**
 * bare-bones implemenation of run args. output regex strings as well as chrome
 * start in ranges for testing.
 */
function run(...[arg]: string[]) {
    const isDryRun = arg === "dryRun";
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

    if (!isDryRun) {
        writeFileSync(OUTPUT_PATH, fileStr);

        console.log(`Browser regexes saved to: ${OUTPUT_PATH}\n`);
    } else {
        console.log("dry run");
        console.log(`${OUTPUT_PATH} =>`);
        console.log(fileStr);
    }
}

run(...process.argv.slice(2));
