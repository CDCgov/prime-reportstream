import { screen } from "@testing-library/react";
import { useLocation } from "react-router-dom";

import { USNavLink } from "../../components/USLink";
import { render } from "../../utils/Test/render";

import SideNavItem from "./SideNavItem";

const mockedUseLocation = vi.mocked(useLocation);

describe("SideNavItem", () => {
    test("custom component", () => {
        render(
            <SideNavItem href="/foo" to="a">
                Test
            </SideNavItem>,
        );
        const parentLink = screen.getByRole("link");
        expect(parentLink).toHaveTextContent("Test");
    });

    test("no items", () => {
        render(<SideNavItem href="/foo">Test</SideNavItem>);
        const parentLink = screen.getByRole("link");
        expect(parentLink).toHaveTextContent("Test");
    });

    describe("with items", () => {
        test("renders", () => {
            mockedUseLocation.mockReturnValue({
                ...window.location,
                pathname: "/foo/bar",
            } satisfies Partial<ReturnType<typeof useLocation>> as any);
            render(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                >
                    Test
                </SideNavItem>,
            );
            const items = screen.getAllByRole("link");
            const [parentLink, childLink] = items;
            expect(items).toHaveLength(2);
            expect(parentLink).toHaveTextContent("Test");
            expect(childLink).toHaveTextContent("Sub test");
        });

        test("active route", () => {
            mockedUseLocation.mockReturnValue({
                ...window.location,
                pathname: "/foo/bar",
            } satisfies Partial<ReturnType<typeof useLocation>> as any);
            render(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                >
                    Test
                </SideNavItem>,
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeInTheDocument();
        });

        test("inactive route", () => {
            render(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                >
                    Test
                </SideNavItem>,
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeUndefined();
        });

        test("forced active", () => {
            render(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                    isActive={true}
                >
                    Test
                </SideNavItem>,
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeInTheDocument();
        });

        test("forced inactive", () => {
            render(
                <SideNavItem
                    href="/foo"
                    items={[<USNavLink href="/foo/bar">Sub test</USNavLink>]}
                    isActive={false}
                >
                    Test
                </SideNavItem>,
            );
            const [, childLink] = screen.getAllByRole("link");
            expect(childLink).toBeUndefined();
        });
    });
});
