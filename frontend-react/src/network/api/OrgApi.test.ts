import { orgApi } from "./OrgApi";

describe("Organization API", () => {
    test("getOrgList", () => {
        const endpoint = orgApi.getOrgList();
        expect(endpoint).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations`,
            headers: {
                Authorization: "Bearer ",
                Organization: "",
            },
            responseType: "json",
        });
    });

    test("getOrgDetail", () => {
        const endpoint = orgApi.getOrgDetail("test");
        expect(endpoint).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/test`,
            headers: {
                Authorization: "Bearer ",
                Organization: "",
            },
            responseType: "json",
        });
    });

    test("getSenderDetail", () => {
        const endpoint = orgApi.getSenderDetail("test", "testSender");
        expect(endpoint).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/test/senders/testSender`,
            headers: {
                Authorization: "Bearer ",
                Organization: "",
            },
            responseType: "json",
        });
    });
});
