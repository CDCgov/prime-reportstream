import { Endpoint } from "rest-hooks";
import { Api } from "./Api";

jest.mock("../../components/GlobalContextProvider", () => ({
    getStoredOktaToken: () => { return "test token" },
    getStoredOrg: () => { return "test org" }
}))

describe("Api.ts", () => {

    test("static members have values", () => {
        expect(Api.accessToken).toBe("test token")
        expect(Api.organization).toBe("test org")
        expect(Api.baseUrl).toBe("/api")
    })

    // How can I test the Axios instance? :thinking:

    test("generateEndpoint() creates valid endpoint", () => {
        const testEndpoint = Api.generateEndpoint("/test/url", Api)

        expect(testEndpoint.url).toBe("/test/url")
        expect(testEndpoint.api).toBe(Api)
    });

}) 
