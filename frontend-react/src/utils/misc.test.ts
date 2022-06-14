import OrgSettingsBaseResource from "../resources/OrgSettingsBaseResource";

import {
    formatDate,
    getErrorDetailFromResponse,
    getVersionWarning,
    splitOn,
    VersionWarningType,
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
    meta: {
        version: 5,
        createdBy: "test@example.com",
        createdAt: "1/1/2000 00:00",
    },
};

test("getVersionWarning test", async () => {
    const fullWarning = getVersionWarning(VersionWarningType.FULL, objResource);
    expect(fullWarning).toContain(objResource.meta?.createdBy);

    const popupWarning = getVersionWarning(VersionWarningType.POPUP);
    expect(popupWarning).toContain("WARNING!");
});

test("formatDate test", () => {
    expect(formatDate("2022-06-12T22:22:53.833Z")).toBe(
        "Sun, 6/12/2022, 3:22 PM"
    );
    console.error = jest.fn(); // we KNOW the next call complains with a console.error(). don't let it stop the test
    expect(formatDate("bad date")).toBe("bad date");
});
