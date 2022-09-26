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
 * NOTICE: the root element does NOT have a key/keyEnd because it's the root.
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
 */

/* these are exposed via SourceMapResult. Note: root element never has key/keyEnd */
type PointerProp = "value" | "valueEnd" | "key" | "keyEnd";

interface Location {
    line: number;
    column: number;
    pos: number;
}

type Pointers = Record<string, Record<PointerProp, Location>>;

export interface SourceMapResult {
    json: string;
    pointers: Pointers;
}

export const jsonSourceMap = (
    data: any,
    spaces: number = 2
): SourceMapResult => {
    let json = "";
    let pointers: Pointers = {};
    const cur: Location = { line: 0, column: 0, pos: 0 };

    if (!isValidType(data)) {
        // throw error?
        return {
            json: "",
            pointers: {},
        };
    }

    // whitespace
    const wspaceLoc: Location = { line: 0, column: 0, pos: 0 };
    const whitespace = " ".repeat(spaces);

    function isValidType(data: any) {
        const VALID_TYPES = ["number", "bigint", "boolean", "string", "object"];
        return VALID_TYPES.indexOf(typeof data) >= 0;
    }

    function quote(str: string) {
        const ESC_QUOTE = /"|\\/g;
        const ESC_B = /[\b]/g;
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
        if (whitespace) {
            json += "\n" + whitespace.repeat(lvl);
            cur.line++;
            cur.column = 0;
            while (lvl--) {
                if (wspaceLoc.line) {
                    cur.line += wspaceLoc.line;
                    cur.column = wspaceLoc.column;
                } else {
                    cur.column += wspaceLoc.column;
                }
                cur.pos += wspaceLoc.pos;
            }
            cur.pos += 1; // \n character
        }
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
            const propPtr = ptr + "/" + escapeJsonPointer(key);
            indent(propLvl);
            map(propPtr, "key");
            out(quote(key));
            map(propPtr, "keyEnd");
            out(":");
            if (whitespace) {
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
        console.error(err);
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
                console.log("unknown type");
        }
        map(ptr, "valueEnd");
    };

    stringifyRecursive(data, 0, "");

    return {
        json,
        pointers,
    };
};
