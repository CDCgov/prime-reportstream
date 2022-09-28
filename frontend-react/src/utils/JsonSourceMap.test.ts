import { jsonSourceMap } from "./JsonSourceMap";

// <editor-fold defaultstate="collapsed" desc="mockData: based on real data">
const realData = {
    resourceType: "ValueSet",
    id: "2.16.840.1.114222.4.11.7536",
    text: {
        status: "generated",
        div: '<div xmlns="http://www.w3.org/1999/xhtml">Subject\'s height units</div>',
    },
    url: "https://phinvads.cdc.gov/baseStu3/ValueSet/PHVS_HeightUnit_CDC",
    identifier: [
        {
            system: "urn:oid:2.16.840.1.114222.4.11.7536",
        },
    ],
    version: "1",
    name: "PHVS_HeightUnit_CDC",
    title: "Height Unit (CDC)",
    status: "active",
    experimental: false,
    date: "2016-08-24",
    publisher: "CDC NNDSS (https://www.cdc.gov/nndss/contact.html)",
    contact: [
        {
            telecom: [
                {
                    system: "url",
                    value: "https://www.cdc.gov/nndss/index.html",
                },
            ],
        },
    ],
    description:
        '<div xmlns="http://www.w3.org/1999/xhtml">Subject\'s height units</div>',
    compose: {
        lockedDate: "2016-08-24",
        include: [
            {
                system: "https://ucum.org",
                version: "20171130",
                concept: [
                    {
                        code: "[in_us]",
                        display: "inch [length]",
                    },
                    {
                        code: "cm",
                        display: "CentiMeter [SI Length Units]",
                    },
                ],
            },
        ],
    },
};
// </editor-fold>

describe("JsonSourceMap test suite", () => {
    jest.spyOn(global.console, "error");

    test("JsonSourceMap Basic no quotes", () => {
        const data = { a: 1, b: 2 };
        const result = jsonSourceMap(data, 2);

        // stringify should match standard
        expect(result.json).toEqual(JSON.stringify(data, null, 2));

        const keys = Object.keys(result.pointers);
        const values = Object.values(result.pointers);

        const totalLen = result.json.length;

        // checking overall should be good enough to sanity check all the content
        expect(keys).toStrictEqual(["", "/a", "/b"]);
        // Everything inside the {}
        expect(values[0].value.pos).toBe(0);
        expect(values[0].valueEnd.pos).toBe(totalLen);
        expect(values).toStrictEqual([
            {
                value: {
                    line: 0,
                    column: 0,
                    pos: 0,
                },
                valueEnd: {
                    line: 3,
                    column: 1,
                    pos: 22,
                },
            },
            {
                key: {
                    line: 1,
                    column: 2,
                    pos: 4,
                },
                keyEnd: {
                    line: 1,
                    column: 5,
                    pos: 7,
                },
                value: {
                    line: 1,
                    column: 7,
                    pos: 9,
                },
                valueEnd: {
                    line: 1,
                    column: 8,
                    pos: 10,
                },
            },
            {
                key: {
                    line: 2,
                    column: 2,
                    pos: 14,
                },
                keyEnd: {
                    line: 2,
                    column: 5,
                    pos: 17,
                },
                value: {
                    line: 2,
                    column: 7,
                    pos: 19,
                },
                valueEnd: {
                    line: 2,
                    column: 8,
                    pos: 20,
                },
            },
        ]);
    });

    test("JsonSourceMap format matches JSON.stringify", () => {
        const result = jsonSourceMap(realData, 2);
        // run through json parser and make sure it's valid
        const reparsed = JSON.parse(result.json);
        const checkStrOriginal = JSON.stringify(realData, null, 2);
        const checkStrResult = JSON.stringify(reparsed, null, 2);
        expect(checkStrOriginal).toEqual(checkStrResult);

        expect(jsonSourceMap(realData, 4).json).toEqual(
            JSON.stringify(realData, null, 4)
        );
    });

    test("JsonSourceMap Basic quotes", () => {
        const data = { key: "value" };
        const result = jsonSourceMap(data, 2);

        // stringify should match standard
        expect(result.json).toEqual(JSON.stringify(data, null, 2));

        const keys = Object.keys(result.pointers);
        const values = Object.values(result.pointers);

        // checking overall should be good enough to sanity check all the content
        expect(keys).toStrictEqual(["", "/key"]);
        // Everything inside the {}
        expect(values[0].value).toStrictEqual({ line: 0, column: 0, pos: 0 });
        expect(values[0].valueEnd).toStrictEqual({
            line: 2,
            column: 1,
            pos: result.json.length,
        });

        expect(values[1].value).toStrictEqual({ line: 1, column: 9, pos: 11 });
        expect(values[1].valueEnd).toStrictEqual({
            line: 1,
            column: 16,
            pos: 18,
        });
    });

    test("JsonSourceMap numeric types test", () => {
        const data = {
            data: [
                -1, 0, 1, 2, 3, 3.14, 0.12, 1.3e-12, 3.14e22,
                2305843009213694000,
            ],
        };
        const result = jsonSourceMap(data, 2);
        // stringify should match standard
        expect(result.json).toEqual(JSON.stringify(data, null, 2));

        const keys = Object.keys(result.pointers);
        const values = Object.values(result.pointers);
        expect(keys).toStrictEqual([
            "",
            "/data",
            "/data/0",
            "/data/1",
            "/data/2",
            "/data/3",
            "/data/4",
            "/data/5",
            "/data/6",
            "/data/7",
            "/data/8",
            "/data/9",
        ]);

        // start element should always be this
        expect(keys[0]).toBe("");
        expect(values[0].value.line).toBe(0);
        expect(values[0].value.pos).toBe(0);

        // each number gets its own "value" entry. spot check the scientific notation and bigint entries.

        // 0.12
        expect(values[8]).toStrictEqual({
            value: {
                line: 8,
                column: 4,
                pos: 64,
            },
            valueEnd: {
                line: 8,
                column: 8,
                pos: 68,
            },
        });

        // 1.3e-12
        expect(values[9]).toStrictEqual({
            value: {
                line: 9,
                column: 4,
                pos: 74,
            },
            valueEnd: {
                line: 9,
                column: 11,
                pos: 81,
            },
        });

        // 3.14e22
        expect(values[10]).toStrictEqual({
            value: {
                line: 10,
                column: 4,
                pos: 87,
            },
            valueEnd: {
                line: 10,
                column: 12,
                pos: 95,
            },
        });

        // BigInt
        expect(values[11]).toStrictEqual({
            value: {
                line: 11,
                column: 4,
                pos: 101,
            },
            valueEnd: {
                line: 11,
                column: 23,
                pos: 120,
            },
        });

        // now check the whole length last (most likely failures were checked in prior lines)
        expect(values[0].valueEnd).toStrictEqual({
            line: 13,
            column: 1,
            pos: result.json.length,
        });
    });

    test("JsonSourceMap basic array", () => {
        const data = { bray: [{ a: "a1", b: "b2" }] };
        const result = jsonSourceMap(data, 2);
        // stringify should match standard
        expect(result.json).toEqual(JSON.stringify(data, null, 2));

        const keys = Object.keys(result.pointers);
        const values = Object.values(result.pointers);

        expect(keys).toStrictEqual([
            "",
            "/bray",
            "/bray/0",
            "/bray/0/a",
            "/bray/0/b",
        ]);

        // checking overall should be good enough to sanity check all the content
        expect(values[0]).toStrictEqual({
            value: {
                line: 0,
                column: 0,
                pos: 0,
            },
            valueEnd: {
                line: 7,
                column: 1,
                pos: result.json.length,
            },
        });
    });

    test("JsonSourceMap empty", () => {
        const data = { emptyObj: {}, emptyArray: [], emptyStr: "" };
        const result = jsonSourceMap(data, 2);
        // stringify should match standard
        const expected = JSON.stringify(data, null, 2);
        expect(result.json).toEqual(expected);

        const keys = Object.keys(result.pointers);
        const values = Object.values(result.pointers);

        expect(keys).toStrictEqual([
            "",
            "/emptyObj",
            "/emptyArray",
            "/emptyStr",
        ]);

        // checking overall should be good enough to sanity check all the content
        // <editor-fold defaultstate="collapsed" desc="expect(values).toStrictEqual">
        expect(values).toStrictEqual([
            {
                value: {
                    line: 0,
                    column: 0,
                    pos: 0,
                },
                valueEnd: {
                    line: 4,
                    column: 1,
                    pos: result.json.length,
                },
            },
            {
                key: {
                    line: 1,
                    column: 2,
                    pos: 4,
                },
                keyEnd: {
                    line: 1,
                    column: 12,
                    pos: 14,
                },
                value: {
                    line: 1,
                    column: 14,
                    pos: 16,
                },
                valueEnd: {
                    line: 1,
                    column: 16,
                    pos: 18,
                },
            },
            {
                key: {
                    line: 2,
                    column: 2,
                    pos: 22,
                },
                keyEnd: {
                    line: 2,
                    column: 14,
                    pos: 34,
                },
                value: {
                    line: 2,
                    column: 16,
                    pos: 36,
                },
                valueEnd: {
                    line: 2,
                    column: 18,
                    pos: 38,
                },
            },
            {
                key: {
                    line: 3,
                    column: 2,
                    pos: 42,
                },
                keyEnd: {
                    line: 3,
                    column: 12,
                    pos: 52,
                },
                value: {
                    line: 3,
                    column: 14,
                    pos: 54,
                },
                valueEnd: {
                    line: 3,
                    column: 16,
                    pos: 56,
                },
            },
        ]);
        // </editor-fold>
    });

    test("check children", () => {
        const data = {
            name: "smith",
            coins: [2, 5, 1],
            children: [
                { name: "kid3", age: 3 },
                { name: "kid1", age: 0 },
                { name: "kid2", age: 2 },
            ],
        };
        const result = jsonSourceMap(data, 2);
        // stringify should match standard
        const expected = JSON.stringify(data, null, 2);
        expect(result.json).toEqual(expected);

        const keys = Object.keys(result.pointers);
        const values = Object.values(result.pointers);
        expect(keys).toStrictEqual([
            "",
            "/name",
            "/coins",
            "/coins/0",
            "/coins/1",
            "/coins/2",
            "/children",
            "/children/0",
            "/children/0/name",
            "/children/0/age",
            "/children/1",
            "/children/1/name",
            "/children/1/age",
            "/children/2",
            "/children/2/name",
            "/children/2/age",
        ]);
    });

    test("throws if json include non-standard Set/Map objects", () => {
        expect(() =>
            jsonSourceMap(
                new Map([
                    ["key1", "value1"],
                    ["key2", "value2"],
                ]),
                2
            )
        ).toThrow();
    });
});
