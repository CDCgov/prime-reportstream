import React from "react";
import { BrowserRouter as Router } from "react-router-dom";

import { SessionContext } from "../../contexts/SessionContext";
import { MemberType } from "../../utils/OrganizationUtils";

import ReportStreamHeader from "./ReportStreamHeader";

export default {
    title: "Components/Navbar",
    component: ReportStreamHeader,
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

export const NavbarDefault = (): React.ReactElement => <ReportStreamHeader />;

export const NavbarBlueVariant = (): React.ReactElement => (
    <ReportStreamHeader blueVariant={true} />
);

export const LoggedInNavbarDefault = (): React.ReactElement => (
    <ReportStreamHeader />
);

LoggedInNavbarDefault.decorators = [
    (Story: React.FC) => (
        <SessionContext.Provider
            value={
                {
                    activeMembership: {
                        parsedName: "Test Co",
                        memberType: MemberType.PRIME_ADMIN,
                    },
                    user: {
                        claims: { email: "test@testing.com", sub: "" },
                        isUserAdmin: true,
                        isAdminStrictCheck: true,
                        isUserReceiver: false,
                        isUserSender: false,
                        isUserTransceiver: false,
                    },
                    logout: () => undefined,
                    setActiveMembership: () => undefined,
                } as any
            }
        >
            <Story />
        </SessionContext.Provider>
    ),
];

export const LoggedInNavbarBlueVariant = (): React.ReactElement => (
    <ReportStreamHeader blueVariant={true} />
);

LoggedInNavbarBlueVariant.decorators = [
    (Story: React.FC) => (
        <SessionContext.Provider
            value={
                {
                    activeMembership: {
                        parsedName: "Test Co",
                        memberType: MemberType.PRIME_ADMIN,
                    },
                    user: {
                        isUserAdmin: true,
                        isAdminStrictCheck: true,
                        isUserReceiver: false,
                        isUserSender: false,
                        isUserTransceiver: false,
                        claims: { email: "test@testing.com", sub: "" },
                    },
                    logout: () => undefined,
                    setActiveMembership: () => undefined,
                } as any
            }
        >
            <Story />
        </SessionContext.Provider>
    ),
];
