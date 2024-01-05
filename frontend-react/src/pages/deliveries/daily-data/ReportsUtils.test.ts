import config from "../../../config";

import { reportDetailURL } from "./ReportsUtils";

describe("ReportsUtils", () => {
    test("reportDetailURL", () => {
        const url = reportDetailURL("1");
        expect(url).toEqual(`${config.API_ROOT}/history/report/1`);
        const urlWithBase = reportDetailURL("1", "http://localhost:3000");
        expect(urlWithBase).toEqual(
            "http://localhost:3000/api/history/report/1",
        );
    });
});
