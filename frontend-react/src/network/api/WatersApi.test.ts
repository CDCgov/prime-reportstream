import { SimpleError } from "../../utils/UsefulTypes";

import { createRequestConfig, RSRequestConfig } from "./NewApi";
import WatersApi from "./WatersApi";

describe("Waters API", () => {
    test("postReport", () => {
        const config: RSRequestConfig | SimpleError = createRequestConfig<{
            org: string;
            sender: string;
        }>(WatersApi, "waters", "POST", "[token]", "test-org");

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
