import { convert } from "html-to-text";

/**
 * splitOn('foo', 1);
 * // ["f", "oo"]
 *
 * splitOn([1, 2, 3, 4], 2);
 * // [[1, 2], [3, 4]]
 *
 * splitOn('fooBAr', 1, 4);
 * //  ["f", "ooB", "Ar"]
 */
export const splitOn: {
    <T = string>(s: T, ...i: number[]): T[];
    <T extends any[]>(s: T, ...i: number[]): T[];
} = <T>(slicable: string | T[], ...indices: number[]) => [0, ...indices].map((n, i, m) => slicable.slice(n, m[i + 1]));

/**
 * @param jsonTextValue
 * @return { valid: boolean; offset: number; errorMsg: string}  If valid = false, then offset is where the error is.
 *  valid=true, offset: -1, errorMsg: "" - this is to keep typechecking simple for the caller. offset is always a number
 */
export const checkJson = (jsonTextValue: string): { valid: boolean; offset: number; errorMsg: string } => {
    try {
        JSON.parse(jsonTextValue);
        return { valid: true, offset: -1, errorMsg: "" };
    } catch (err: any) {
        // message like `'Unexpected token _ in JSON at position 164'`
        // or           `Unexpected end of JSON input`
        const errorMsg = err?.message || "unknown error";

        // parse out the position and try to select it for them.
        // NOTE: if "at position N" string not found, then assume mistake is at the end
        let offset = jsonTextValue.length;
        const findPositionMatch = errorMsg?.matchAll(/position (\d+)/gi)?.next();
        if (findPositionMatch?.value?.length === 2) {
            const possibleOffset = parseInt(findPositionMatch.value[1] || -1);
            if (!isNaN(possibleOffset) && possibleOffset !== -1) {
                offset = possibleOffset;
            }
        }
        return { valid: false, offset, errorMsg };
    }
};

export function isValidServiceName(text: string): boolean {
    return /^[a-z\d-_]+$/i.test(text);
}

/**
 * returns the error detail usually found in the "error" field of the JSON returned
 * otherwise, just return the general exception detail
 */
export async function getErrorDetailFromResponse(e: any) {
    const errorResponse = await e?.response?.json();
    return errorResponse?.error ? errorResponse.error : e.toString();
}

export enum VersionWarningType {
    POPUP = "popup",
    FULL = "full",
}

/**
 * returns a customized message in the case of trying to edit the wrong version of a setting
 * @param warningType either POPUP (for the toast notification) or FULL (for the red text on the compare modal itself)
 * @param settings the resource object from which to use information helpful to the user
 */
export function getVersionWarning(warningType: VersionWarningType, settings: any = null): string {
    switch (warningType) {
        case VersionWarningType.POPUP:
            return "WARNING! A newer version of this setting now exists in the database";
        case VersionWarningType.FULL:
            return `WARNING! A change has been made to the setting you're trying to update by 
                    '${
                        settings?.createdBy || "UNKNOWN"
                    }'. Please coordinate with that user and return to update the setting 
                    again, if needed`;
    }
}

export function formatDate(date: string | Date): string {
    // 'Thu, 3/31/2022, 4:50 AM'
    // Note that this returns Epoch when receiving a null date string
    return new Intl.DateTimeFormat("en-US", {
        weekday: "short",
        year: "numeric",
        month: "numeric",
        day: "numeric",
        hour: "numeric",
        minute: "numeric",
    }).format(new Date(date));
}

/*
  for strings in machine-readable form:
    * camel cased
    * inconsistent caps
    * whitespace delimited by - or _

  translate into normal human-readable strings with all words capitalized
*/
export const toHumanReadable = (machineString: string): string => {
    const delimitersToSpaces = machineString.replace(/[_-]/g, " ");
    const camelcaseToSpaces = delimitersToSpaces.replace(/([A-Z])/g, " $1");
    const fixCaps = camelcaseToSpaces.replace(
        /(?:\s|^)(\w)/g,
        (_match: string, capture: string) => ` ${capture.toUpperCase()}`,
    );
    return fixCaps.trim();
};

// ... capitalizes the first letter in a string
export const capitalizeFirst = (uncapped: string): string => {
    if (!uncapped?.length) {
        return uncapped;
    }
    const newFirst = uncapped[0].toUpperCase();
    return `${newFirst}${uncapped.substring(1)}`;
};

/**
 * use: `groupBy(struct[], s => s.name)`
 *
 * @param array
 * @param predicate
 */
export const groupBy = <T>(array: T[], predicate: (value: T, index: number, array: T[]) => string) =>
    array.reduce(
        (acc, value, index, array) => {
            (acc[predicate(value, index, array)] ||= []).push(value);
            return acc;
        },
        {} as Record<string, T[]>,
    );

/* Takes a url that contains the 'report/' location and returns
    the folder location, sending org, and filename
*/
export const parseFileLocation = (
    urlFileLocation: string,
): {
    folderLocation: string;
    sendingOrg: string;
    fileName: string;
} => {
    const fileReportsLocation = urlFileLocation.split("/").pop() ?? "";
    const [folderLocation, sendingOrg, fileName] = fileReportsLocation.split("%2F");

    if (!(folderLocation && sendingOrg && fileName)) {
        return {
            folderLocation: "",
            sendingOrg: "",
            fileName: "",
        };
    }

    return {
        folderLocation,
        sendingOrg,
        fileName,
    };
};

export const removeHTMLFromString = (input: string, options = {}) => {
    return convert(input, options);
};

export const convertCase = (str: string, inputCase: string, outputCase: string) => {
    let words;

    // break the original string into an array of lowercase words
    switch (inputCase) {
        case "camel":
        case "pascal":
            words = str
                .replace(/([A-Z])/g, " $1")
                .trim()
                .toLowerCase()
                .split(/\s+/);
            break;
        case "snake":
            words = str.toLowerCase().split("_");
            break;
        case "kebab":
            words = str.toLowerCase().split("-");
            break;
        case "constant":
            words = str.toLowerCase().split("_");
            break;
        default:
            throw new Error(`Unknown inputCase: "${inputCase}"`);
    }

    let result;
    switch (outputCase) {
        case "sentence":
            result = words.join(" ");
            if (result.length > 0) {
                result = result.charAt(0).toUpperCase() + result.slice(1);
            }
            break;

        case "title":
            result = words.map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(" ");
            break;

        default:
            throw new Error(`Unknown outputCase: "${outputCase}"`);
    }

    return result;
};

export const prettifyJSON = (str: string) => {
    let prettyStr = str;

    try {
        const parsed = JSON.parse(str);
        prettyStr = JSON.stringify(parsed, null, 2);
    } catch (e) {
        console.warn("Invalid JSON:", e);
    }
    return prettyStr;
};

export const removeFileExtension = (filename: string) => {
    // Replace the last dot and everything following it, if present, with ""
    return filename.replace(/\.[^/.]+$/, "");
};
