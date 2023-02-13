import { getIsIE } from "./GetIsIE";

describe("GetIsIE", () => {
    test("detects ie 10", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)"
            )
        ).toBeTruthy();
    });
    test("detects ie 11", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko"
            )
        ).toBeTruthy();
    });
});
