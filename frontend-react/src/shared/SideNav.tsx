import { SideNav } from "@trussworks/react-uswds";
import React from "react";
import { useLocation, RouteObject } from "react-router-dom";
import { ReadonlyDeep } from "type-fest";

import { appRoutes } from "../AppRouter";
import { USNavLink } from "../components/USLink";
import { matchRoute } from "../utils/misc";
import { RSRouteObject } from "../utils/UsefulTypes";

export interface SideNavItemProps
    extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
    items?: React.ReactNode[];
    children: React.ReactNode;
    to?:
        | string
        | React.FunctionComponent<
              React.AnchorHTMLAttributes<HTMLAnchorElement> & {
                  children: React.ReactNode;
              }
          >;
    isActive?: boolean;
}

/**
 * Wrapper component that provides the NavLink and SideNav sibling elements (if items
 * provided). The parent navlink can be customized by passing a functional component
 * or html element string as the "to" prop (defaults to USNavLink). The SideNav sibling
 * can be forced visible via the "isActive" prop (otherwise defaults to checking path).
 */
export default function SideNavItem({
    href,
    children,
    items,
    to: NavLink = USNavLink,
    isActive,
    ...props
}: SideNavItemProps) {
    const { pathname } = useLocation();
    const isSubnavVisible =
        isActive !== undefined
            ? isActive
            : href === undefined ||
              href === "" ||
              pathname === href ||
              pathname.startsWith(href);
    const subnavClassname = !isSubnavVisible ? "display-none" : "";

    // SideNav doesn't allow custom classes so we have to wrap in a div 😡
    return (
        <>
            <NavLink href={href} {...props}>
                {children}
            </NavLink>
            {items ? (
                <div className={subnavClassname} aria-hidden={!isSubnavVisible}>
                    <SideNav isSubnav={true} items={items} />
                </div>
            ) : undefined}
        </>
    );
}

export interface SideNavRouteItemBase {
    route: RSRouteObject;
    children?: SideNavRouteItemBase[];
    isActive?: boolean;
}

export type SideNavRouteItem =
    | SideNavRouteItemBase
    | ReadonlyDeep<SideNavRouteItemBase>
    | (ReadonlyDeep<SideNavRouteItemBase> & { children: SideNavRouteItem[] });

export function createSideNavItem(
    { route, isActive, children }: SideNavRouteItem,
    currentPath?: string
) {
    const lineage =
        currentPath ??
        matchRoute(appRoutes as unknown as RouteObject[], route as RouteObject)
            ?.path;

    if (!lineage) {
        return undefined;
    }

    const pathname = `${lineage}${
        !(route.path?.startsWith("#") ?? false) ? "/" : ""
    }${route.path ?? ""}`.replaceAll(/\/\/+/g, "/");

    const { path } = route;
    const url = new URL(pathname, window.location.origin);
    const href = `${url.pathname}${url.hash}${url.search}`;
    if (children?.length) {
        return (
            <SideNavItem
                key={path}
                href={href}
                isActive={isActive}
                items={children?.map((i: any) => createSideNavItem(i))}
            >
                {route.title}
            </SideNavItem>
        );
    }

    return <USNavLink href={href}>{route.title}</USNavLink>;
}
