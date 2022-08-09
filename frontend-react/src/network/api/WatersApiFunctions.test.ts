import { watersServer } from "../../__mocks__/WatersMockServer";

import { WatersResponse } from "./WatersApi";
import watersApiFunctions from "./WatersApiFunctions";

describe("test all hooks and methods", () => {
    beforeAll(() => watersServer.listen());
    afterEach(() => watersServer.resetHandlers());
    afterAll(() => watersServer.close());

    test("postReport returns expected success data", async () => {
        const successData: WatersResponse = await watersApiFunctions.postData(
            "test.default",
            "test-file.hl7",
            "application/hl7-v2",
            "fileContents",
            "test-org",
            "[token]",
            "waters"
        );
        expect(successData.id).toEqual("uuid-string");
    });

    test("postReport returns expected errors and warnings data", async () => {
        const failureData: WatersResponse = await watersApiFunctions.postData(
            "bad-client",
            "test-file.hl7",
            "application/hl7-v2",
            "badData",
            "test-org",
            "[token]",
            "waters"
        );
        expect(failureData.id).toEqual(null);
        expect(failureData.errorCount).toEqual(1);
        expect(failureData.warningCount).toEqual(0);
        if (failureData.errors) {
            expect(failureData.errors[0].message).toEqual(
                "The GMT offset hour value of the TM datatype must be >=0 and <=23"
            );
        }
    });

    test("postReport returns 500 error data", async () => {
        const failureData: WatersResponse = await watersApiFunctions.postData(
            "give me a very bad response",
            "test-file.hl7",
            "application/hl7-v2",
            "",
            "test-org",
            "[token]",
            "waters"
        );
        expect(failureData.ok).toEqual(false);
        if (failureData.errors) {
            expect(failureData.errors[0]["details"]).toEqual(
                "This response will not parse and will cause an error"
            );
        }
    });
});
