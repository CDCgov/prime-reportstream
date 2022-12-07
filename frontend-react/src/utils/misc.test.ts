import OrgSettingsBaseResource from "../resources/OrgSettingsBaseResource";

import {
    formatDate,
    getErrorDetailFromResponse,
    getVersionWarning,
    splitOn,
    VersionWarningType,
    toHumanReadable,
    capitalizeFirst,
    groupBy,
    checkJson,
} from "./misc";
import { mockEvent } from "./TestUtils";

test("splitOn test", () => {
    const r1 = splitOn("foo", 1);
    expect(JSON.stringify(r1)).toBe(`["f","oo"]`);

    const r2 = splitOn([1, 2, 3, 4], 2);
    expect(JSON.stringify(r2)).toBe(`[[1,2],[3,4]]`);

    const r3 = splitOn("fooBAr", 1, 4);
    expect(JSON.stringify(r3)).toBe(`["f","ooB","Ar"]`);

    // boundary conditions
    const r4 = splitOn("fooBAr", 0, 6);
    expect(JSON.stringify(r4)).toBe(`["","fooBAr",""]`);
});

test("verify checking json for errors", () => {
    expect(checkJson(`{}`)).toStrictEqual({
        valid: true,
        offset: -1,
        errorMsg: "",
    });
    expect(checkJson(`{`)).toStrictEqual({
        valid: false,
        offset: 1,
        errorMsg: "Unexpected end of JSON input",
    });
    expect(checkJson(`{ "foo": [1,2,3 }`)).toStrictEqual({
        valid: false,
        offset: 16,
        errorMsg: "Unexpected token } in JSON at position 16",
    });
});

const mockErrorEvent = mockEvent({
    response: {
        json: () => {
            return { error: "fail fail fail" };
        },
    },
});

test("getErrorDetailFromResponse test", async () => {
    const error = await getErrorDetailFromResponse(mockErrorEvent);
    expect(error).toBe("fail fail fail");
});

const objResource: OrgSettingsBaseResource = {
    name: "test setting",
    url: "http://localhost",
    pk: () => "10101",
    version: 5,
    createdBy: "test@example.com",
    createdAt: "1/1/2000 00:00",
};

test("getVersionWarning test", async () => {
    const fullWarning = getVersionWarning(VersionWarningType.FULL, objResource);
    expect(fullWarning).toContain(objResource.createdBy);

    const popupWarning = getVersionWarning(VersionWarningType.POPUP);
    expect(popupWarning).toContain("WARNING!");
});

test("formatDate test", () => {
    expect(formatDate("2022-06-12T12:23:30.833Z")).toContain(
        "Sun, 6/12/2022, " // time part fails because of timezone of server
    );

    expect(formatDate("2022-06-12T12:23:30.833Z")).toContain(
        ":23" // check the minutes are at least correct
    );

    console.warn = jest.fn(); // we KNOW the next call complains with a console.warn(). don't let it stop the test
    expect(formatDate("bad date")).toBe("bad date");
});

describe("toHumanReadable", () => {
    test("fixes up a string delimited by underscores", () => {
        expect(toHumanReadable("hi_there_people")).toEqual("Hi There People");
    });

    test("fixes up a string delimited by dashes", () => {
        expect(toHumanReadable("hi-there-people")).toEqual("Hi There People");
    });

    test("fixes up a camel case string", () => {
        expect(toHumanReadable("hiTherePeople")).toEqual("Hi There People");
    });
});

describe("capitalizeFirst", () => {
    test("returns something falsey if there isn't anything to capitalize", () => {
        expect(capitalizeFirst("")).toEqual("");
    });
    test("returns original string with first letter capitalized", () => {
        expect(capitalizeFirst("all")).toEqual("All");
        expect(capitalizeFirst("vdpoiENUpajfPWEOIWA")).toEqual(
            "VdpoiENUpajfPWEOIWA"
        );
    });
});

describe("groupBy ", () => {
    test("groupBy basic", () => {
        expect(
            groupBy(
                ["one", "two", "three", "four", "five"],
                (v) => `${v.length}`
            )
        ).toStrictEqual({
            3: ["one", "two"],
            4: ["four", "five"],
            5: ["three"],
        });
    });
});
