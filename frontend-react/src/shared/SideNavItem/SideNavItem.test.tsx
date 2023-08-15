import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";
import { USNavLink } from "../../components/USLink";

import SideNavItem from "./SideNavItem";

describe("SideNavItem", () => {
    test("custom component", () => {
        renderApp(
            <SideNavItem href="/foo" to="a">
                Test
            </SideNavItem>,
        );
        const parentLink = screen.getByRole("link");
        expect(parentLink).toHaveTextContent("Test");
    });

    test("no items", () => {
        renderApp(<SideNavItem href="/foo">Test</SideNavItem>);
        const parentLink = screen.getByRole("link");
        expect(parentLink).toHaveTextContent("Test");
    });

    describe("with items", () => {
        test("renders", () => {
            renderApp(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                >
                    Test
                </SideNavItem>,
                {
                    initialRouteEntries: ["/foo"],
                },
            );
            const items = screen.getAllByRole("link");
            const [parentLink, childLink] = items;
            expect(items).toHaveLength(2);
            expect(parentLink).toHaveTextContent("Test");
            expect(childLink).toHaveTextContent("Sub test");
        });

        test("active route", () => {
            renderApp(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                >
                    Test
                </SideNavItem>,
                {
                    initialRouteEntries: ["/foo"],
                },
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeInTheDocument();
        });

        test("inactive route", () => {
            renderApp(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                >
                    Test
                </SideNavItem>,
                {
                    initialRouteEntries: ["/elsewhere"],
                },
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeUndefined();
        });

        test("forced active", () => {
            renderApp(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                    isActive={true}
                >
                    Test
                </SideNavItem>,
                {
                    initialRouteEntries: ["/elsewhere"],
                },
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeInTheDocument();
        });

        test("forced inactive", () => {
            renderApp(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                    isActive={false}
                >
                    Test
                </SideNavItem>,
                {
                    initialRouteEntries: ["/foo"],
                },
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeUndefined();
        });
    });
});
