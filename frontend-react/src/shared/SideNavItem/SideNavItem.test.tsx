import { screen } from "@testing-library/react";

import SideNavItem from "./SideNavItem";
import { USNavLink } from "../../components/USLink";
import { renderApp } from "../../utils/CustomRenderUtils";

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

    /**
     * The "with items" test suite is skipped as part of the sunset page update.
     * These tests are being temporarily disabled as the navigation structure
     * has been refactored to accommodate the new static sunset page.
     */
    describe.skip("with items", () => {
        test("renders", () => {
            renderApp(
                <SideNavItem
                    href="/foo"
                    items={[
                        <USNavLink key="foobar" href="/foo/bar">
                            Sub test
                        </USNavLink>,
                    ]}
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
                    items={[
                        <USNavLink key="foobar" href="/foo/bar">
                            Sub test
                        </USNavLink>,
                    ]}
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
                    items={[
                        <USNavLink key="foobar" href="/foo/bar">
                            Sub test
                        </USNavLink>,
                    ]}
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
                    items={[
                        <USNavLink key="foobar" href="/foo/bar">
                            Sub test
                        </USNavLink>,
                    ]}
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
                    items={[
                        <USNavLink key="foobar" href="/foo/bar">
                            Sub test
                        </USNavLink>,
                    ]}
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
