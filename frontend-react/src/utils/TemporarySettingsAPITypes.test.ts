import { getListOfEnumValues, SampleObject } from "./TemporarySettingsAPITypes";

class TestSampleObject extends SampleObject {
    title = "test";
    getAllEnums(): Map<string, string[]> {
        return new Map([["format", getListOfEnumValues("format")]]);
    }
    description(): string {
        return "A test sample object";
    }
}

describe("Settings API Types", () => {
    test("SampleObject methods", () => {
        const obj = new TestSampleObject();
        expect(obj.stringify()).toEqual(
            JSON.stringify({ title: "test" }, null, 6)
        );
        expect(obj.getAllEnums()).toEqual(
            new Map([["format", ["CSV", "HL7"]]])
        );
        expect(obj.description()).toEqual("A test sample object");
    });

    test("getListOfEnumValues", () => {
        expect(getListOfEnumValues("customerStatus")).toEqual([
            "inactive",
            "testing",
            "active",
        ]);
    });
});
