import { SimpleError } from "../../utils/UsefulTypes";
import config from "../../config";

import { createRequestConfig, RSRequestConfig } from "./NewApi";
import WatersApi from "./WatersApi";

const { RS_API_URL } = config;

describe("Waters API", () => {
    test("postReport", () => {
        const config: RSRequestConfig | SimpleError = createRequestConfig<{
            org: string;
            sender: string;
        }>(WatersApi, "waters", "POST", "[token]", "test-org");

        expect(config).toEqual({
            method: "POST",
            url: `${RS_API_URL}/api/waters`,
            headers: {
                authorization: "Bearer [token]",
                organization: "test-org",
                "authentication-type": "okta",
            },
        });
    });
});
