import {
    externalUrls,
    internalUrls,
    nonRouteUrls,
    routeUrls,
    routeUrlsMap,
} from "./__mocks__";
import { getHrefRoute, isExternalUrl } from "./utils";

describe("getHrefRoute", () => {
    test.each(routeUrls)("'%s' returns string", (url) => {
        expect(getHrefRoute(url)).toBe(routeUrlsMap[url] ?? url);
    });
    test.each(nonRouteUrls)("'%s' returns undefined", (url) => {
        expect(getHrefRoute(url)).toBe(undefined);
    });
});

describe("isExternalUrl", () => {
    test.each(externalUrls)("'%s' returns true", (url) => {
        expect(isExternalUrl(url)).toBeTruthy();
    });

    test.each(internalUrls)("'%s' returns false", (url) => {
        expect(isExternalUrl(url)).toBeFalsy();
    });
});
