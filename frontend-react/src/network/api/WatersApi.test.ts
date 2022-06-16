import { watersApi } from "./WatersApi";

describe("Waters API", () => {
    test("postReport", () => {
        const endpoint = watersApi.postReport(
            "test.default",
            "test-data.hl7",
            "application/hl7-v2"
        );
        expect(endpoint).toEqual({
            method: "POST",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/waters`,
            headers: {
                Authorization: "Bearer ",
                "Content-Type": "application/hl7-v2",
                Organization: "",
                "authentication-type": "okta",
                client: "test.default",
                payloadName: "test-data.hl7",
            },
            responseType: "json",
        });
    });
});
