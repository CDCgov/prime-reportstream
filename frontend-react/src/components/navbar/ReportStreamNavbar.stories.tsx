import React from "react";

import { ReportStreamNavbar } from "./ReportStreamNavbar";

export default {
    title: "Components/Navbar",
    component: ReportStreamNavbar,
};

export const NavbarDefault = (): React.ReactElement => <ReportStreamNavbar />;

export const NavbarBlueVariant = (): React.ReactElement => (
    <ReportStreamNavbar blueVariant={true} />
);
