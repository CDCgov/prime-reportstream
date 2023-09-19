import {
    transformFacilityTypeClass,
    transformFacilityTypeLabel,
} from "./DataDashboardUtils";

describe("transformFacilityTypeLabel", () => {
    test("returns a transformed facility name", () => {
        expect(transformFacilityTypeLabel("facility")).toBe(
            "PERFORMING FACILITY",
        );

        expect(transformFacilityTypeLabel("FACILITY")).toBe(
            "PERFORMING FACILITY",
        );

        expect(transformFacilityTypeLabel("provider")).toBe(
            "ORDERING PROVIDER",
        );

        expect(transformFacilityTypeLabel("submitter")).toBe("SUBMITTER");
    });

    test("returns the facility name if it is not a valid SenderType", () => {
        expect(transformFacilityTypeLabel("Sender")).toBe("Sender");
    });
});

describe("transformFacilityTypeClass", () => {
    test("returns the class from the facility name", () => {
        expect(transformFacilityTypeClass("facility")).toBe(
            "font-mono-3xs radius-md padding-05 height-3 bg-mint-cool-5v",
        );

        expect(transformFacilityTypeClass("provider")).toBe(
            "font-mono-3xs radius-md padding-05 height-3 bg-indigo-cool-10v",
        );

        expect(transformFacilityTypeClass("submitter")).toBe(
            "font-mono-3xs radius-md padding-05 height-3 bg-magenta-10v",
        );
    });

    test("does not return a class if the facility is not a valid SenderType", () => {
        expect(transformFacilityTypeClass("Sender")).toBe("");
    });
});
