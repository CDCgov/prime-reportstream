import { jsonSourceMap } from "./JsonSourceMap";

// <editor-fold defaultstate="collapsed" desc="mockData: realData">
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
    test("JsonSourceMap Basic test", () => {
        const data = { key: "value" };
        const result = jsonSourceMap(data, 2);

        // stringify should match standard
        expect(result.json).toEqual(JSON.stringify(data, null, 2));

        const keys = Object.keys(result.pointers);
        const values = Object.values(result.pointers);

        // checking overall should be good enough to sanity check all the content
        expect(keys).toStrictEqual(["", "/key"]);
        expect(values[0].value).toStrictEqual({ line: 0, column: 0, pos: 0 });
        expect(values[0].valueEnd).toStrictEqual({
            line: 2,
            column: 1,
            pos: 18,
        });
    });

    test("JsonSourceMap Basic test", () => {
        const result2 = jsonSourceMap(realData, 2);
        // run through json parser and make sure it's valid
        const reparsed = JSON.parse(result2.json);
        const checkStrOriginal = JSON.stringify(realData, null, 2);
        const checkStrResult = JSON.stringify(reparsed, null, 2);
        expect(checkStrOriginal).toEqual(checkStrResult);
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
        expect(values[8].value).toStrictEqual({ line: 8, column: 0, pos: 34 });
        expect(values[8].valueEnd).toStrictEqual({
            line: 8,
            column: 4,
            pos: 38,
        });

        // 1.3e-12
        expect(values[9].value).toStrictEqual({ line: 9, column: 0, pos: 40 });
        expect(values[9].valueEnd).toStrictEqual({
            line: 9,
            column: 7,
            pos: 47,
        });

        // 3.14e22
        expect(values[10].value).toStrictEqual({
            line: 10,
            column: 0,
            pos: 49,
        });
        expect(values[10].valueEnd).toStrictEqual({
            line: 10,
            column: 8,
            pos: 57,
        });

        // BigInt
        expect(values[11].value).toStrictEqual({
            line: 11,
            column: 0,
            pos: 59,
        });
        expect(values[11].valueEnd).toStrictEqual({
            line: 11,
            column: 19,
            pos: 78,
        });

        // now check the whole length last (most likely failures were checked in prior lines)
        expect(values[0].valueEnd).toStrictEqual({
            line: 13,
            column: 1,
            pos: 82,
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
                pos: 40,
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
                    pos: 52,
                },
            },
            {
                key: {
                    line: 1,
                    column: 0,
                    pos: 2,
                },
                keyEnd: {
                    line: 1,
                    column: 10,
                    pos: 12,
                },
                value: {
                    line: 1,
                    column: 12,
                    pos: 14,
                },
                valueEnd: {
                    line: 1,
                    column: 14,
                    pos: 16,
                },
            },
            {
                key: {
                    line: 2,
                    column: 0,
                    pos: 18,
                },
                keyEnd: {
                    line: 2,
                    column: 12,
                    pos: 30,
                },
                value: {
                    line: 2,
                    column: 14,
                    pos: 32,
                },
                valueEnd: {
                    line: 2,
                    column: 16,
                    pos: 34,
                },
            },
            {
                key: {
                    line: 3,
                    column: 0,
                    pos: 36,
                },
                keyEnd: {
                    line: 3,
                    column: 10,
                    pos: 46,
                },
                value: {
                    line: 3,
                    column: 12,
                    pos: 48,
                },
                valueEnd: {
                    line: 3,
                    column: 14,
                    pos: 50,
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
        debugger;
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

        // <editor-fold defaultstate="collapsed" desc="expect(values).toStrictEqual">
        expect(values).toStrictEqual([
            {
                value: {
                    line: 0,
                    column: 0,
                    pos: 0,
                },
                valueEnd: {
                    line: 21,
                    column: 1,
                    pos: 147,
                },
            },
            {
                key: {
                    line: 1,
                    column: 0,
                    pos: 2,
                },
                keyEnd: {
                    line: 1,
                    column: 6,
                    pos: 8,
                },
                value: {
                    line: 1,
                    column: 8,
                    pos: 10,
                },
                valueEnd: {
                    line: 1,
                    column: 15,
                    pos: 17,
                },
            },
            {
                key: {
                    line: 2,
                    column: 0,
                    pos: 19,
                },
                keyEnd: {
                    line: 2,
                    column: 7,
                    pos: 26,
                },
                value: {
                    line: 2,
                    column: 9,
                    pos: 28,
                },
                valueEnd: {
                    line: 6,
                    column: 1,
                    pos: 39,
                },
            },
            {
                value: {
                    line: 3,
                    column: 0,
                    pos: 30,
                },
                valueEnd: {
                    line: 3,
                    column: 1,
                    pos: 31,
                },
            },
            {
                value: {
                    line: 4,
                    column: 0,
                    pos: 33,
                },
                valueEnd: {
                    line: 4,
                    column: 1,
                    pos: 34,
                },
            },
            {
                value: {
                    line: 5,
                    column: 0,
                    pos: 36,
                },
                valueEnd: {
                    line: 5,
                    column: 1,
                    pos: 37,
                },
            },
            {
                key: {
                    line: 7,
                    column: 0,
                    pos: 41,
                },
                keyEnd: {
                    line: 7,
                    column: 10,
                    pos: 51,
                },
                value: {
                    line: 7,
                    column: 12,
                    pos: 53,
                },
                valueEnd: {
                    line: 20,
                    column: 1,
                    pos: 145,
                },
            },
            {
                value: {
                    line: 8,
                    column: 0,
                    pos: 55,
                },
                valueEnd: {
                    line: 11,
                    column: 1,
                    pos: 83,
                },
            },
            {
                key: {
                    line: 9,
                    column: 0,
                    pos: 57,
                },
                keyEnd: {
                    line: 9,
                    column: 6,
                    pos: 63,
                },
                value: {
                    line: 9,
                    column: 8,
                    pos: 65,
                },
                valueEnd: {
                    line: 9,
                    column: 14,
                    pos: 71,
                },
            },
            {
                key: {
                    line: 10,
                    column: 0,
                    pos: 73,
                },
                keyEnd: {
                    line: 10,
                    column: 5,
                    pos: 78,
                },
                value: {
                    line: 10,
                    column: 7,
                    pos: 80,
                },
                valueEnd: {
                    line: 10,
                    column: 8,
                    pos: 81,
                },
            },
            {
                value: {
                    line: 12,
                    column: 0,
                    pos: 85,
                },
                valueEnd: {
                    line: 15,
                    column: 1,
                    pos: 113,
                },
            },
            {
                key: {
                    line: 13,
                    column: 0,
                    pos: 87,
                },
                keyEnd: {
                    line: 13,
                    column: 6,
                    pos: 93,
                },
                value: {
                    line: 13,
                    column: 8,
                    pos: 95,
                },
                valueEnd: {
                    line: 13,
                    column: 14,
                    pos: 101,
                },
            },
            {
                key: {
                    line: 14,
                    column: 0,
                    pos: 103,
                },
                keyEnd: {
                    line: 14,
                    column: 5,
                    pos: 108,
                },
                value: {
                    line: 14,
                    column: 7,
                    pos: 110,
                },
                valueEnd: {
                    line: 14,
                    column: 8,
                    pos: 111,
                },
            },
            {
                value: {
                    line: 16,
                    column: 0,
                    pos: 115,
                },
                valueEnd: {
                    line: 19,
                    column: 1,
                    pos: 143,
                },
            },
            {
                key: {
                    line: 17,
                    column: 0,
                    pos: 117,
                },
                keyEnd: {
                    line: 17,
                    column: 6,
                    pos: 123,
                },
                value: {
                    line: 17,
                    column: 8,
                    pos: 125,
                },
                valueEnd: {
                    line: 17,
                    column: 14,
                    pos: 131,
                },
            },
            {
                key: {
                    line: 18,
                    column: 0,
                    pos: 133,
                },
                keyEnd: {
                    line: 18,
                    column: 5,
                    pos: 138,
                },
                value: {
                    line: 18,
                    column: 7,
                    pos: 140,
                },
                valueEnd: {
                    line: 18,
                    column: 8,
                    pos: 141,
                },
            },
        ]);
        // </editor-fold>
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
