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
    isValidServiceName,
    parseFileLocation,
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
        "Sun, 6/12/2022, ", // time part fails because of timezone of server
    );

    expect(formatDate("2022-06-12T12:23:30.833Z")).toContain(
        ":23", // check the minutes are at least correct
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
            "VdpoiENUpajfPWEOIWA",
        );
    });
});

describe("groupBy ", () => {
    test("groupBy basic", () => {
        expect(
            groupBy(
                ["one", "two", "three", "four", "five"],
                (v) => `${v.length}`,
            ),
        ).toStrictEqual({
            3: ["one", "two"],
            4: ["four", "five"],
            5: ["three"],
        });
    });
});

describe("isValidServiceName", () => {
    describe("when the string is empty or contains characters outside of the allowed set", () => {
        const names = [
            "",
            "test/",
            "te.st",
            "te$t",
            "tes#",
            "@est",
            "te,st",
            "te)",
            "a\\ttest",
            "|test",
            "test hello",
        ];

        names.forEach((name) => {
            test(`return false for string ${name}`, () => {
                expect(isValidServiceName(name)).toEqual(false);
            });
        });
    });

    describe("when the string only contains characters within the allowed set", () => {
        const names = [
            "orgname",
            "org-name",
            "org_name",
            "ORG-NAME",
            "org-name-1",
        ];

        names.forEach((name) => {
            test(`returns true for string ${name}`, () => {
                expect(isValidServiceName(name)).toEqual(true);
            });
        });
    });
});

describe("parseFileLocation", () => {
    test("returns the folder location, sending org, and filename when valid url", () => {
        const { folderLocation, sendingOrg, fileName } = parseFileLocation(
            "https://azurite:10000/devstoreaccount1/reports/receive%2Fsimple_report.csvuploader%2Fupload-covid-19-c33f9d36-9e5b-44eb-9368-218d88f3a7d1-20230131190253.csv",
        );
        expect(folderLocation).toEqual("receive");
        expect(sendingOrg).toEqual("simple_report.csvuploader");
        expect(fileName).toEqual(
            "upload-covid-19-c33f9d36-9e5b-44eb-9368-218d88f3a7d1-20230131190253.csv",
        );
    });

    test("returns empty strings for sendingOrg and fileName when string is missing all three fragments split on %2F", () => {
        const { folderLocation, sendingOrg, fileName } = parseFileLocation(
            "https://azurite:10000/devstoreaccount1/reports/receive%2Fupload-covid-19-c33f9d36-9e5b-44eb-9368-218d88f3a7d1-20230131190253.csv",
        );
        expect(folderLocation).toEqual("");
        expect(sendingOrg).toEqual("");
        expect(fileName).toEqual("");
    });

    test("returns empty strings for sendingOrg and fileName when string is missing %2F", () => {
        const { folderLocation, sendingOrg, fileName } = parseFileLocation(
            "https://azurite:10000/devstoreaccount1/reports/receive",
        );
        expect(folderLocation).toEqual("");
        expect(sendingOrg).toEqual("");
        expect(fileName).toEqual("");
    });
});
