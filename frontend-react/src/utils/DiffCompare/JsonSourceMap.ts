/**
 * Fork of abandoned project https://github.com/epoberezkin/json-source-map#readme
 * MIT License: https://github.com/epoberezkin/json-source-map/blob/master/LICENSE
 *
 * Updated to typescript. es6 assumptions. Removed support for native Map and Set
 * (which can end up in json built via code). If you're calling and get errors, then
 * do a normalization first via JSON.parse(JSON.Stringify(data)) which convers these
 * types and deals to key/value and arrays
 *
 * Find text offsets of json parts, this enables hightlight sections.
 * Part of the process needs to normalize the json so the offsets are correct.
 * so this fixed json string is returned as part of the process.
 *
 * NOTICE: the root element does NOT have a key/keyEnd because it's the root. Its key is ""
 *
 * input
 * `{foo:'bar'}`
 *
 * Result
 * json:
 * `{
 *   "foo": "bar"
 * }`
 *
 * pointers:
 * { '':
 *    { value: { line: 0, column: 0, pos: 0 },
 *      valueEnd: { line: 2, column: 1, pos: 18 } },
 *   '/foo':
 *    { key: { line: 1, column: 2, pos: 4 },
 *      keyEnd: { line: 1, column: 7, pos: 9 },
 *      value: { line: 1, column: 9, pos: 11 },
 *      valueEnd: { line: 1, column: 14, pos: 16 } } }
 *
 * Also see: https://www.json.org/json-en.html
 *
 * Special cases:
 *  1. keys that already have `/` in them will be converted to `~1`
 *  2. Since the above subs in ~... if a `~` appears it become `~0`
 *  This isn't ideal, but it's probably not very common.
 */

/* these are exposed via SourceMapResult. Note: root element never has key/keyEnd */
type PointerProp = "value" | "valueEnd" | "key" | "keyEnd";

interface JsonMapLocation {
    line: number;
    column: number;
    pos: number;
}

type JsonMapPointers = Record<string, Record<PointerProp, JsonMapLocation>>;

export interface SourceMapResult {
    json: string;
    pointers: JsonMapPointers;
}

/**
 * @param jsonD Valid Json Object
 * @param spaces Number of spaces to use in the normalized JSON string returned.
 * @return A normalized json string and a list of "pointers" that are offsets into it.
 */
export const jsonSourceMap = (
    jsonD: unknown,
    spaces: number = 2,
): SourceMapResult => {
    // left and right should be json objects, but there's really no way to typescript enforce it.
    if (typeof jsonD === "string") {
        console.warn("Did you mean to pass simple strings versus json objects");
    }

    let json = "";
    let pointers: JsonMapPointers = {};
    const cur: JsonMapLocation = { line: 0, column: 0, pos: 0 };

    if (!isValidType(jsonD)) {
        // throw error?
        return {
            json: "",
            pointers: {},
        };
    }

    // whitespace
    const whitespace = " ".repeat(spaces);

    function isValidType(data: any) {
        const VALID_TYPES = ["number", "bigint", "boolean", "string", "object"];
        return VALID_TYPES.indexOf(typeof data) >= 0;
    }

    function quote(str: string) {
        const ESC_QUOTE = /["\\]/g;
        const ESC_B = /[\b]/g; // ignore ide linter, keep []
        const ESC_F = /\f/g;
        const ESC_N = /\n/g;
        const ESC_R = /\r/g;
        const ESC_T = /\t/g;

        str = str
            .replace(ESC_QUOTE, "\\$&")
            .replace(ESC_F, "\\f")
            .replace(ESC_B, "\\b")
            .replace(ESC_N, "\\n")
            .replace(ESC_R, "\\r")
            .replace(ESC_T, "\\t");
        return `"${str}"`;
    }

    function escapeJsonPointer(str: string) {
        const ESC_0 = /~/g;
        const ESC_1 = /\//g;
        return str.replace(ESC_0, "~0").replace(ESC_1, "~1");
    }

    function out(str: string) {
        cur.column += str.length;
        cur.pos += str.length;
        json += str;
    }

    function indent(lvl: number) {
        if (!spaces) {
            return;
        }
        json += "\n" + whitespace.repeat(lvl);
        cur.line++;
        cur.column = 0;
        while (lvl--) {
            cur.column += spaces;
            cur.pos += spaces;
        }
        cur.pos++; // \n character
    }

    function map(ptr: string, prop: PointerProp) {
        pointers[ptr] = pointers[ptr] || {};
        pointers[ptr][prop] = {
            line: cur.line,
            column: cur.column,
            pos: cur.pos,
        };
    }

    const stringifyArray = (data: any, lvl: number, ptr: string) => {
        if (Object.keys(data).length === 0) {
            out("[]");
            return;
        }
        out("[");
        let itemLvl = lvl + 1;
        for (let i = 0; i < data.length; i++) {
            if (i) {
                out(",");
            }
            indent(itemLvl);
            const item = isValidType(data[i]) ? data[i] : null;
            const itemPtr = `${ptr}/${i}`;
            stringifyRecursive(item, itemLvl, itemPtr);
        }
        indent(lvl);
        out("]");
    };

    function stringifyObject(data: any, lvl: number, ptr: string) {
        if (Object.keys(data).length === 0) {
            out("{}");
            return;
        }
        out("{");
        const propLvl = lvl + 1;
        let count = 0;
        for (const [key, value] of Object.entries(data)) {
            if (!isValidType(value)) {
                continue;
            }
            if (count) {
                out(",");
            }
            // "/" is root, but we don't want "//key", just "/key"
            const propPtr = [ptr, escapeJsonPointer(key)].join("/");
            indent(propLvl);
            map(propPtr, "key");
            out(quote(key));
            map(propPtr, "keyEnd");
            out(":");
            if (spaces) {
                out(" ");
            }
            stringifyRecursive(value, propLvl, propPtr);
            count++;
        }
        indent(lvl);
        out("}");
    }

    function stringifyMapOrSet(_data: any, _lvl: number, _ptr: string) {
        /* this logic was problematic and doesn't appear if you get json back from an api,
         but only when js CODE creates the json in memory using Sets and Maps.

           example (set acts like map):
           JSON.stringify([...new Set([ ["key1", "value1"],
                                        ["key1", "value3b"]])]);
           > '[["key1","value1"],["key1","value3b"]]'

            JSON.stringify([...new Map([ ["key1", "value1"],
                                        ["key1", "value3b"]])]);
           > '[["key1","value3b"]]'

           The correct way to deal with this is to just normalize the js before
           calling this code. This code could always do this normalization, but it
           added a constant overhead for an edge case.

            JSON.parse(JSON.stringify([...setData]))
         */
        const err =
            "Map and Set elements not supported. Normalize json first using `JSON.parse(JSON.stringify([...data]))`";
        console.warn(err);
        throw new SyntaxError(`${err} Line: ${cur.line}  Pos: ${cur.pos}`);
    }

    const stringifyRecursive = (data: any, lvl: number, ptr: string) => {
        map(ptr, "value");
        switch (typeof data) {
            case "number": // includes NaN
            case "bigint":
            case "boolean":
                out(data.toString());
                break;
            case "string":
                out(quote(data));
                break;
            case "object": // include null
                if (data === null) {
                    out("null");
                } else if (typeof data.toJSON === "function") {
                    out(quote(data.toJSON()));
                } else if (
                    Array.isArray(data) ||
                    data.constructor.BYTES_PER_ELEMENT // e.g. Uint8Array([0,1,2,3]); shouldn't exist for normalized
                ) {
                    stringifyArray(data, lvl, ptr);
                } else if (data instanceof Map || data instanceof Set) {
                    stringifyMapOrSet(data, lvl, ptr);
                } else {
                    stringifyObject(data, lvl, ptr);
                }
                break;
            default:
                console.error("unknown type");
        }
        map(ptr, "valueEnd");
    };

    stringifyRecursive(jsonD, 0, "");

    return {
        json,
        pointers,
    };
};
