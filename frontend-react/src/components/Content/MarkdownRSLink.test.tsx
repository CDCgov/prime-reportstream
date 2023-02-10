import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { isExternalUrl, MarkdownRSLink } from "./MarkdownRSLink";

const externalUrls = [
    "https://www.google.com",
    "//www.google.com",
    "//google.com",
];

const internalUrls = [
    undefined,
    "",
    "/login",
    "#",
    "mailto:",
    "login",
    "https://reportstream.cdc.gov/login",
    "https://www.cdc.gov",
    "https://cdc.gov",
    "//reportstream.cdc.gov/login",
    "//www.cdc.gov",
    "//cdc.gov",
];

describe("isExternalUrl", () => {
    test.each(externalUrls)("'%s' returns true", (url) => {
        expect(isExternalUrl(url)).toBeTruthy();
    });

    test.each(internalUrls)("'%s' returns false", (url) => {
        expect(isExternalUrl(url)).toBeFalsy();
    });
});

describe("MarkdownSRLink", () => {
    test.each(externalUrls)("'%s' returns external link", (url) => {
        const view = renderWithRouter(
            <MarkdownRSLink node={undefined as any} href={url}>
                Test
            </MarkdownRSLink>
        );
        expect(view.container.children[0]).toHaveClass("usa-link--external");
    });

    test.each(internalUrls)("'%s' returns internal link", (url) => {
        const view = renderWithRouter(
            <MarkdownRSLink node={undefined as any} href={url}>
                Test
            </MarkdownRSLink>
        );
        expect(view.container.children[0]).not.toHaveClass(
            "usa-link--external"
        );
    });
});
