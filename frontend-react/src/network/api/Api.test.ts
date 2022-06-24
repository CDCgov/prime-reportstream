import { API_ROOT, Api, EndpointConfig } from "./Api";

type ApiItem = string | string[];

const fakeHeaders = {
    Authorization: "Bearer [token]",
};

class TestApi extends Api {
    /* Get array of test data */
    getTestList(): EndpointConfig<ApiItem> {
        return super.configure<ApiItem>({
            method: "GET",
            url: this.basePath,
        });
    }
}

const mockRegisterApi = jest.fn();

jest.mock("../Apis", () => ({
    registerApi: (args: unknown) => mockRegisterApi(args),
}));

describe("Api", () => {
    let testApi = new TestApi("test"); // this is to work around typing
    beforeEach(() => {
        testApi = new TestApi("test");
    });
    test("Constructor assigns default properties", () => {
        expect(testApi.root).toEqual(API_ROOT);
        expect(testApi.headers).toEqual({});
    });

    test("Endpoints generate correctly", () => {
        expect(testApi.getTestList()).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/test`,
            headers: {},
            responseType: "json",
        });
    });

    describe("generateUrl", () => {
        test("returns root plus passed string as path", () => {
            expect(testApi.generateUrl("anything")).toEqual(
                `${API_ROOT}/anything`
            );
        });
    });

    describe("configure", () => {
        test("returns expected param based configuration", () => {
            expect(
                testApi.configure<unknown>({
                    method: "POST",
                    url: "thisCouldBeAnything",
                    headers: {
                        fakeHeader: "notReal",
                    },
                    responseType: "text",
                })
            ).toEqual({
                url: `${API_ROOT}/thisCouldBeAnything`,
                method: "POST",
                headers: {
                    fakeHeader: "notReal",
                },
                responseType: "text",
            });
        });
    });

    describe("updateSession", () => {
        test("updates Api instance as expected", () => {
            expect(testApi.headers).toEqual({});
            testApi.updateSession(fakeHeaders);
            expect(testApi.headers).toEqual(fakeHeaders);
        });
    });

    describe("register", () => {
        test("calls registerApi", () => {
            mockRegisterApi.mockClear();
            expect(mockRegisterApi).toHaveBeenCalledTimes(0);
            testApi.register();
            expect(mockRegisterApi).toHaveBeenCalledTimes(1);
        });

        test("is called on instantiation", () => {
            expect(mockRegisterApi).toHaveBeenCalledTimes(1);
        });
    });
});
