import { API } from "../NewApi";

export class MyApiItem {
    testField: string;
    constructor(testField: string) {
        this.testField = testField;
    }
}

export const MyApi: API = {
    resource: MyApiItem,
    baseUrl: "/api/test",
    endpoints: new Map([
        [
            "list",
            {
                url: "/test",
                methods: ["GET"],
            },
        ],
        [
            "badList",
            {
                url: "/badList",
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
