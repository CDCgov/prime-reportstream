import { getIsIE } from "./GetIsIE";

describe("GetIsIE", () => {
    test("detects ie 10", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)",
            ),
        ).toBeTruthy();
    });
    test("detects ie 11", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko",
            ),
        ).toBeTruthy();
    });
    test("false for edge", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36 Edg/110.0.1587.41",
            ),
        );
    });
    test("false for chrome", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
            ),
        ).toBeFalsy();
    });
    test("false for firefox", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/109.0",
            ),
        ).toBeFalsy();
    });
    test("false for safari", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Safari/605.1.15",
            ),
        ).toBeFalsy();
    });
    test("false for iOS", () => {
        expect(
            getIsIE(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Mobile/15E148 Safari/604.1",
            ),
        ).toBeFalsy();
    });
});
