import { Api, Endpoint } from "./Api";
import { OrgApi } from "./OrgApi";

describe("OrgApi.ts", () => {
    test("implements static members from API", () => {
        expect(OrgApi.accessToken).toBe(Api.accessToken);
        expect(OrgApi.organization).toBe(Api.organization);
        expect(OrgApi.config).toBe(Api.config);
    });

    test("detail endpoint integrity check", () => {
        const testId: string = "123";
        const detailEndpoint: Endpoint = OrgApi.detail(testId);

        expect(detailEndpoint.url).toBe(`${OrgApi.baseUrl}/${testId}`);
    });
});
