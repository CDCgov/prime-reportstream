import { watersApi } from "./WatersApi";

describe("Waters API", () => {
    const fakeHeaders = {
        Authorization: "Bearer [token]",
        Organization: "test-org",
    };
    test("postReport", () => {
        watersApi.updateSession(fakeHeaders);
        const endpoint = watersApi.postReport(
            "test.default",
            "test-data.hl7",
            "application/hl7-v2"
        );
        expect(endpoint).toEqual({
            method: "POST",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/waters`,
            headers: {
                Authorization: fakeHeaders.Authorization,
                "Content-Type": "application/hl7-v2",
                Organization: fakeHeaders.Organization,
                "authentication-type": "okta",
                client: "test.default",
                payloadName: "test-data.hl7",
            },
            responseType: "json",
        });
    });
});
