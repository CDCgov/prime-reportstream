import { Api, EndpointConfig } from "./Api";

const fakeRoot = `${process.env.REACT_APP_BACKEND_URL}/api`;
const fakeHeaders = {
    headers: {
        Authorization: "Bearer [token]",
    },
};

// const config = new ApiConfig({
//     root: `${process.env.REACT_APP_BACKEND_URL}/api`,
//     headers: {
//         Authorization: "Bearer [token]",
//     },
// });

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
const testApi = new TestApi("test");

describe("Api", () => {
    test("Constructor assigns properties", () => {
        expect(testApi.config.root).toEqual(config.root);
        expect(testApi.config.headers).toEqual(config.headers);
    });

    test("Endpoints generate correctly", () => {
        expect(testApi.getTestList()).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/test`,
            headers: {
                Authorization: "Bearer [token]",
            },
            responseType: "json",
        });
    });
});
