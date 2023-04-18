import { SideNav } from "@trussworks/react-uswds";
import React from "react";

import { createSideNavItem, SideNavRouteItem } from "../../../shared/SideNav";

import {
    gettingStartedNav,
    reportStreamApiNav,
    documentationNav,
} from "./routes";

const sidenavItems: SideNavRouteItem[] = [
    {
        route: reportStreamApiNav,
        children: reportStreamApiNav.children.map((c) => ({ route: c })),
    },
    {
        route: gettingStartedNav,
        children: gettingStartedNav.children.map((c) => ({ route: c })),
    },
    {
        route: documentationNav,
        children: documentationNav.children.map((c) => ({ route: c })),
        isActive: true,
    },
];

export function Sidenav() {
    const items = sidenavItems.map((i) => createSideNavItem(i));
    return <SideNav items={items} />;
}
