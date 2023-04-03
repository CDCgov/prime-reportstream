import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import { SideNavItem } from "./SideNav";
import { USNavLink } from "./USLink";

// link -> li -> sidenav (ul) -> div
function getIsVisible(e: HTMLElement) {
    return !e.parentElement?.parentElement?.parentElement?.classList.contains(
        "display-none"
    );
}

describe("SideNavItem", () => {
    test("custom component", () => {
        renderApp(
            <SideNavItem href="/foo" to="a">
                Test
            </SideNavItem>
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
                }
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
                }
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(getIsVisible(childLink)).toBeTruthy();
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
                }
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(getIsVisible(childLink)).not.toBeTruthy();
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
                }
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(getIsVisible(childLink)).toBeTruthy();
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
                }
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(getIsVisible(childLink)).not.toBeTruthy();
        });
    });
});
