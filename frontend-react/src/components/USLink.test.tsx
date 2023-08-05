import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import {
    getHrefRoute,
    USCrumbLink,
    USExtLink,
    USLink,
    USNavLink,
    USLinkButton,
    SafeLink,
    USSmartLink,
    isExternalUrl,
} from "./USLink";

const enumProps = {
    size: ["big"],
    accentStyle: ["cool", "warm"],
};

const enumPropMap = {
    size: "",
    accentStyle: "accent",
};

const testScenarios = Object.entries(enumProps).map(([key, valueList]) =>
    valueList.map((v) => [key, v, enumPropMap[key as keyof typeof enumProps]]),
);

const routeUrls = [
    "",
    "/",
    "asdf",
    `//${window.location.host}/asdf`,
    `${window.location.origin}`,
    "#",
    "#asdf",
    "##asdf",
];

const routeUrlsMap = {
    [`//${window.location.host}/asdf`]: "/asdf",
    [`${window.location.origin}`]: `/`,
};

const mailToLink = "mailto:someone@abc.com";

const nonRouteUrls = [
    undefined,
    mailToLink,
    "https://www.google.com",
    "http://www.google.com",
    "//www.google.com",
];

describe("getHrefRoute", () => {
    test.each(routeUrls)("'%s' returns string", (url) => {
        expect(getHrefRoute(url)).toBe(routeUrlsMap[url] ?? url);
    });
    test.each(nonRouteUrls)("'%s' returns undefined", (url) => {
        expect(getHrefRoute(url)).toBe(undefined);
    });
});

describe("SafeLink", () => {
    // Native react element type will be DOM element name string.
    // Custom components will have a type.displayName of their variable
    // name (ex: const CustomComponent = () => {} will have displayName
    // CustomComponent).
    test.each(routeUrls)("'%s' renders as Link", (url) => {
        const component = SafeLink({ children: <>Test</>, href: url });
        expect(component.type).not.toBe("a");
        expect(component.type.displayName).toBe("Link");
    });
    test.each(nonRouteUrls)("'%s' renders as anchor", (url) => {
        const component = SafeLink({ children: <>Test</>, href: url });
        expect(component.type).toBe("a");
    });
});

describe("USLink", () => {
    test("renders without error", () => {
        renderApp(<USLink href={"/some/url"}>My Link</USLink>);
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-link");
        expect(link).toHaveTextContent("My Link");
    });
    test("renders with additional className values", () => {
        renderApp(
            <USLink href={"/some/url"} className={"my-custom-class"}>
                My Link
            </USLink>,
        );
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-link");
        expect(link).toHaveClass("my-custom-class");
    });
    /** Specialization of USLink */
    describe("USExtLink", () => {
        test("renders with external link class", () => {
            renderApp(<USExtLink href={"/some/url"}>My Link</USExtLink>);
            const link = screen.getByRole("link");
            expect(link).toHaveClass("usa-link");
            expect(link).toHaveClass("usa-link--external");
        });
    });
    /** Specialization of USLink */
    describe("USCrumbLink", () => {
        test("renders with breadcrumb link class", () => {
            renderApp(<USCrumbLink href={"/some/url"}>My Link</USCrumbLink>);
            const link = screen.getByRole("link");
            expect(link).toHaveClass("usa-link");
            expect(link).toHaveClass("usa-breadcrumb__link");
        });
    });
});
/** Specialization of NavLink from react-router-dom */
describe("USNavLink", () => {
    test("renders without error", () => {
        renderApp(<USNavLink href={"/some/url"}>Navigation Link</USNavLink>);
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-nav__link");
    });
    // Not sure that we have a way, nor a need, to test the activeClassName
    // value. It's assumed that react-router-dom is testing that as a part of
    // their package.
});

describe("USLinkButton", () => {
    test("boolean button styles applied", () => {
        renderApp(
            <USLinkButton href="#" secondary base outline inverse unstyled>
                Test
            </USLinkButton>,
        );
        expect(screen.getByRole("link")).toHaveClass(
            "usa-button",
            "usa-button--secondary",
            "usa-button--base",
            "usa-button--outline",
            "usa-button--inverse",
            "usa-button--unstyled",
        );
    });

    test.each(testScenarios)(
        "%s button style applied",
        ([key, value, prefix]) => {
            const prop = { [key]: value };
            const className = prefix
                ? `usa-button--${prefix}-${value}`
                : `usa-button--${value}`;
            renderApp(
                <USLinkButton {...prop} href="#">
                    Test
                </USLinkButton>,
            );
            expect(screen.getByRole("link")).toHaveClass(
                "usa-button",
                className,
            );
        },
    );
});

const externalUrls = [
    "https://www.google.com",
    "//www.google.com",
    "//google.com",
    mailToLink,
];

const internalUrls = [
    undefined,
    "",
    "/login",
    "#",
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

describe("USSmartLink", () => {
    test.each(externalUrls)("'%s' returns external link", (url) => {
        const view = renderApp(<USSmartLink href={url}>Test</USSmartLink>);
        expect(view.container.children[0]).toHaveClass("usa-link--external");
    });

    test.each(internalUrls)("'%s' returns internal link", (url) => {
        const view = renderApp(<USSmartLink href={url}>Test</USSmartLink>);
        expect(view.container.children[0]).not.toHaveClass(
            "usa-link--external",
        );
    });
});
