import { SimpleError } from "../../utils/UsefulTypes";

import { WatersAPI } from "./WatersApi";
import { createRequestConfig, RSRequestConfig } from "./NewApi";

describe("Waters API", () => {
    test("postReport", () => {
        const config: RSRequestConfig | SimpleError = createRequestConfig<{
            org: string;
            sender: string;
        }>(WatersAPI, "waters", "POST", "[token]", "test-org");

        expect(config).toEqual({
            method: "POST",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/waters`,
            headers: {
                authorization: "Bearer [token]",
                organization: "test-org",
                "authentication-type": "okta",
            },
        });
    });
});
