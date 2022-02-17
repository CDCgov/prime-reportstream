import { Endpoint } from "../NetworkTypes";
import { Api } from "./Api";
import { HistoryApi } from "./History";

describe("History.ts", () => {

    test("implements static members from API", () => {
        expect(HistoryApi.accessToken).toBe(Api.accessToken)
        expect(HistoryApi.organization).toBe(Api.organization)
        expect(HistoryApi.instance).toBe(Api.instance)
    });

    test("list endpoint integrity check", () => {
        const listEndpoint: Endpoint = HistoryApi.list()
        expect(listEndpoint.url).toBe(HistoryApi.baseUrl)
        expect(listEndpoint.api).toBe(HistoryApi)
    })

    test("detail endpoint integrity check", () => {
        const testId: string = "123"
        const detailEndpoint: Endpoint = HistoryApi.detail(testId)
        expect(detailEndpoint.url).toBe(`${HistoryApi.baseUrl}/${testId}`)
    })

})
