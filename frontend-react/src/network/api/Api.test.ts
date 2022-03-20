import { Api, ApiConfig, EndpointConfig } from "./Api";

const config = new ApiConfig({
    root: "http://testhost:420/api",
    headers: {
        Authorization: "Bearer [token]",
    },
});

type ApiItem = string | string[];
class TestApi extends Api {
    /* Get array of test data */
    getTestList(): EndpointConfig<ApiItem> {
        return super.configure<ApiItem>({
            method: "GET",
            url: this.basePath,
        });
    }
}
const testApi = new TestApi(config, "test");

describe("ApiConfig", () => {
    test("Constructor assigns properties", () => {
        expect(config.root).toEqual("http://testhost:420/api");
        expect(config.headers).toEqual({
            Authorization: "Bearer [token]",
        });
    });
});

describe("Api", () => {
    test("Constructor assigns properties", () => {
        expect(testApi.config.root).toEqual(config.root);
        expect(testApi.config.headers).toEqual(config.headers);
    });

    test("Endpoints generate correctly", () => {
        expect(testApi.getTestList()).toEqual({
            method: "GET",
            url: "http://testhost:420/api/test",
            headers: {
                Authorization: "Bearer [token]",
            },
            responseType: "json",
        });
    });
});
