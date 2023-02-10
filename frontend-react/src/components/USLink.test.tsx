import { screen } from "@testing-library/react";

import { renderWithRouter } from "../utils/CustomRenderUtils";

import {
    getHrefRoute,
    USCrumbLink,
    USExtLink,
    USLink,
    USNavLink,
} from "./USLink";

const routeUrls = [
    "",
    "#",
    "#asdf",
    "##asdf",
    "/",
    "asdf",
    `//${window.location.host}/asdf`,
    `${window.location.origin}`,
];

const routeUrlsMap = {
    [`//${window.location.host}/asdf`]: "/asdf",
    [`${window.location.origin}`]: `/`,
};

const nonRouteUrls = [
    undefined,
    "mailto:someone@abc.com",
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

describe("USLink", () => {
    test("renders without error", () => {
        renderWithRouter(<USLink href={"/some/url"}>My Link</USLink>);
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-link");
        expect(link).toHaveTextContent("My Link");
    });
    test("renders with additional className values", () => {
        renderWithRouter(
            <USLink href={"/some/url"} className={"my-custom-class"}>
                My Link
            </USLink>
        );
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-link");
        expect(link).toHaveClass("my-custom-class");
    });
    // Native react element type will be DOM element name string.
    // Custom components will have a type.displayName of their variable
    // name (ex: const CustomComponent = () => {} will have displayName
    // CustomComponent).
    test.each(routeUrls)("'%s' renders as Link", (url) => {
        const component = USLink({ children: <>Test</>, href: url });
        expect(component.type).not.toBe("a");
        expect(component.type.displayName).toBe("Link");
    });
    test.each(nonRouteUrls)("'%s' renders as anchor", (url) => {
        const component = USLink({ children: <>Test</>, href: url });
        expect(component.type).toBe("a");
    });
    /** Specialization of USLink */
    describe("USExtLink", () => {
        test("renders with external link class", () => {
            renderWithRouter(<USExtLink href={"/some/url"}>My Link</USExtLink>);
            const link = screen.getByRole("link");
            expect(link).toHaveClass("usa-link");
            expect(link).toHaveClass("usa-link--external");
        });
    });
    /** Specialization of USLink */
    describe("USCrumbLink", () => {
        test("renders with breadcrumb link class", () => {
            renderWithRouter(
                <USCrumbLink href={"/some/url"}>My Link</USCrumbLink>
            );
            const link = screen.getByRole("link");
            expect(link).toHaveClass("usa-link");
            expect(link).toHaveClass("usa-breadcrumb__link");
        });
    });
});
/** Specialization of NavLink from react-router-dom */
describe("USNavLink", () => {
    test("renders without error", () => {
        renderWithRouter(
            <USNavLink href={"/some/url"}>Navigation Link</USNavLink>
        );
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-nav__link");
    });
    // Not sure that we have a way, nor a need, to test the activeClassName
    // value. It's assumed that react-router-dom is testing that as a part of
    // their package.
});
