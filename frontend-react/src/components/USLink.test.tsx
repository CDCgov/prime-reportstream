import { screen } from "@testing-library/react";

import { renderWithRouter } from "../utils/CustomRenderUtils";

import {
    USCrumbLink,
    USExtLink,
    USLink,
    USNavLink,
    USLinkButton,
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
    valueList.map((v) => [key, v, enumPropMap[key as keyof typeof enumProps]])
);

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

describe("USLinkButton", () => {
    test("boolean button styles applied", () => {
        renderWithRouter(
            <USLinkButton secondary base outline inverse unstyled>
                Test
            </USLinkButton>
        );
        expect(screen.getByRole("link")).toHaveClass(
            "usa-button",
            "usa-button--secondary",
            "usa-button--base",
            "usa-button--outline",
            "usa-button--inverse",
            "usa-button--unstyled"
        );
    });

    test.each(testScenarios)(
        "%s=%s button style applied",
        ([key, value, prefix]) => {
            const prop = { [key]: value };
            const className = prefix
                ? `usa-button--${prefix}-${value}`
                : `usa-button--${value}`;
            renderWithRouter(<USLinkButton {...prop}>Test</USLinkButton>);
            expect(screen.getByRole("link")).toHaveClass(
                "usa-button",
                className
            );
        }
    );
});
