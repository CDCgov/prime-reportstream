import { API } from "../NewApi";

export class MyApiItem {
    testField: string;
    constructor(testField: string) {
        this.testField = testField;
    }
}
const MyApi = new API(MyApiItem, "/api/test");
MyApi.addEndpoint("list", "/test", ["GET"]);
MyApi.addEndpoint("badList", "/badList", ["GET"]);
MyApi.addEndpoint("itemById", "/test/:id", [
    "GET",
    "POST",
    "PUT",
    "PATCH",
    "DELETE",
]);

export { MyApi };
