import { API, ApiBaseUrls } from "../NewApi";

export class MyApiItem {
    testField: string;
    constructor(testField: string) {
        this.testField = testField;
    }
}

export const MyApi: API<MyApiItem> = {
    resource: MyApiItem,
    baseUrl: ApiBaseUrls.TEST,
    endpoints: new Map([
        [
            "list",
            {
                url: "/test",
                methods: ["GET"],
            },
        ],
        [
            "itemById",
            {
                url: "/test/:id",
                methods: ["GET", "POST", "PUT", "PATCH", "DELETE"],
            },
        ],
    ]),
};
