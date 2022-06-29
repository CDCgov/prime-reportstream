import { API } from "../NewApi";

export class MyApiItem {
    testField: string;
    constructor(testField: string) {
        this.testField = testField;
    }
}
const MyApi = new API(MyApiItem, "/api/test")
    .addEndpoint("list", "/test", ["GET"])
    .addEndpoint("badList", "/badList", ["GET"])
    .addEndpoint("itemById", "/test/:id", [
        "GET",
        "POST",
        "PUT",
        "PATCH",
        "DELETE",
    ]);

export { MyApi };
