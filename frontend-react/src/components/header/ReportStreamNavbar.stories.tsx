import React from "react";
import { BrowserRouter as Router } from "react-router-dom";

import { SessionContext } from "../../contexts/SessionContext";
import { MemberType } from "../../hooks/UseOktaMemberships";

import { ReportStreamNavbar } from "./ReportStreamNavbar";

export default {
    title: "Components/Navbar",
    component: ReportStreamNavbar,
    parameters: {
        backgrounds: { default: "dark" },
    },
    decorators: [
        (Story: React.FC) => (
            <Router>
                <Story />
            </Router>
        ),
    ],
};

export const NavbarDefault = (): React.ReactElement => <ReportStreamNavbar />;

export const NavbarBlueVariant = (): React.ReactElement => (
    <ReportStreamNavbar blueVariant={true} />
);

export const LoggedInNavbarDefault = (): React.ReactElement => (
    <ReportStreamNavbar />
);

LoggedInNavbarDefault.decorators = [
    (Story: React.FC) => (
        <SessionContext.Provider
            value={{
                isUserAdmin: true,
                activeMembership: {
                    parsedName: "Test Co",
                    memberType: MemberType.PRIME_ADMIN,
                },
                isAdminStrictCheck: true,
                isUserReceiver: false,
                isUserSender: false,
                user: { email: "test@testing.com", sub: "" },
                oktaToken: {},
                dispatch: () => {},
                initialized: false,
                environment: "production",
            }}
        >
            <Story />
        </SessionContext.Provider>
    ),
];

export const LoggedInNavbarBlueVariant = (): React.ReactElement => (
    <ReportStreamNavbar blueVariant={true} />
);

LoggedInNavbarBlueVariant.decorators = [
    (Story: React.FC) => (
        <SessionContext.Provider
            value={{
                isUserAdmin: true,
                activeMembership: {
                    parsedName: "Test Co",
                    memberType: MemberType.PRIME_ADMIN,
                },
                isAdminStrictCheck: true,
                isUserReceiver: false,
                isUserSender: false,
                user: { email: "test@testing.com", sub: "" },
                oktaToken: {},
                dispatch: () => {},
                initialized: false,
                environment: "production",
            }}
        >
            <Story />
        </SessionContext.Provider>
    ),
];
