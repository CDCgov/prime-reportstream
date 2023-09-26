import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { Link, LinkBase } from "./Link";
import {
    externalUrls,
    internalUrls,
    nonRouteUrls,
    routeUrls,
    testScenarios,
} from "./__mocks__";

describe("Link", () => {
    test("renders without error", () => {
        renderApp(<Link href={"/some/url"}>My Link</Link>);
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-link");
        expect(link).toHaveTextContent("My Link");
    });
    test("renders with additional className values", () => {
        renderApp(
            <Link href={"/some/url"} className={"my-custom-class"}>
                My Link
            </Link>,
        );
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-link");
        expect(link).toHaveClass("my-custom-class");
    });

    describe("renders as breadcrumb", () => {
        test("renders with breadcrumb link class", () => {
            renderApp(
                <Link href={"/some/url"} variant="breadcrumb">
                    My Link
                </Link>,
            );
            const link = screen.getByRole("link");
            expect(link).toHaveClass("usa-link");
            expect(link).toHaveClass("usa-breadcrumb__link");
        });
    });

    // Native react element type will be DOM element name string.
    // Custom components will have a type.displayName of their variable
    // name (ex: const CustomComponent = () => {} will have displayName
    // CustomComponent).
    test.each(routeUrls)("'%s' renders as route link", (url) => {
        const component = LinkBase({ children: <>Test</>, href: url });
        expect(component.type).not.toBe("a");
        expect(component.type.displayName).toBe("Link");
    });
    test.each(nonRouteUrls)("'%s' renders as anchor", (url) => {
        const component = LinkBase({ children: <>Test</>, href: url });
        expect(component.type).toBe("a");
    });

    test.each(externalUrls)("'%s' returns external link", (url) => {
        const view = renderApp(<Link href={url}>Test</Link>);
        expect(view.container.children[0]).toHaveClass("usa-link--external");
    });

    test.each(internalUrls)("'%s' returns internal link", (url) => {
        const view = renderApp(<Link href={url}>Test</Link>);
        expect(view.container.children[0]).not.toHaveClass(
            "usa-link--external",
        );
    });

    test("renders as nav link", () => {
        renderApp(
            <Link href={"/some/url"} variant="nav">
                Navigation Link
            </Link>,
        );
        const link = screen.getByRole("link");
        expect(link).toHaveClass("usa-nav__link");
    });

    describe("button variant", () => {
        test("boolean button styles applied", () => {
            renderApp(
                <Link
                    href="#"
                    button={{
                        secondary: true,
                        base: true,
                        outline: true,
                        inverse: true,
                        unstyled: true,
                    }}
                >
                    Test
                </Link>,
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
                    <Link href="#" button={prop}>
                        Test
                    </Link>,
                );
                expect(screen.getByRole("link")).toHaveClass(
                    "usa-button",
                    className,
                );
            },
        );
    });
});

describe("NavLink", () => {});
