import { FC, ReactElement } from "react";
import { BrowserRouter as Router } from "react-router-dom";

import ReportStreamHeader from "./ReportStreamHeader";
import { SessionContext } from "../../contexts/Session/SessionProvider";
import { MemberType } from "../../utils/OrganizationUtils";

export default {
    title: "Components/Navbar",
    component: ReportStreamHeader,
    parameters: {
        backgrounds: { default: "dark" },
    },
    decorators: [
        (Story: FC) => (
            <Router>
                <Story />
            </Router>
        ),
    ],
};

export const NavbarDefault = (): ReactElement => <ReportStreamHeader />;

export const NavbarBlueVariant = (): ReactElement => (
    <ReportStreamHeader blueVariant={true} />
);

export const LoggedInNavbarDefault = (): ReactElement => <ReportStreamHeader />;

LoggedInNavbarDefault.decorators = [
    (Story: FC) => (
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

export const LoggedInNavbarBlueVariant = (): ReactElement => (
    <ReportStreamHeader blueVariant={true} />
);

LoggedInNavbarBlueVariant.decorators = [
    (Story: FC) => (
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
