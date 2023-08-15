import * as OktaReact from "@okta/okta-react";
import { screen } from "@testing-library/react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import { renderApp } from "../../utils/CustomRenderUtils";
import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { RSSessionContext } from "../../contexts/SessionContext";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AccessTokenWithRSClaims } from "../../utils/OrganizationUtils";
import { FeatureName } from "../../AppRouter";

import { ReportStreamHeader } from "./ReportStreamHeader";

const mockAuth = jest.spyOn(OktaReact, "useOktaAuth");
const mockGetUser = jest.fn();

/* Ran into test runner errors that wanted me to wrap "anything
 * altering state" in the `act()` function, but then complained
 * when I wrapped the ting in the act() function... so I made it
 * disappear. */
jest.mock("./SignInOrUser", () => ({
    SignInOrUser: () => {
        return null;
    },
}));

describe("ReportStreamHeader", () => {
    test("renders without errors", () => {
        mockAuth.mockReturnValue({} as IOktaContext);
        mockSessionContext.mockReturnValue({} as RSSessionContext);
        renderApp(<ReportStreamHeader>Test</ReportStreamHeader>);
    });

    test("admins see all", async () => {
        mockAuth.mockReturnValue({
            //@ts-ignore
            oktaAuth: {
                getUser: mockGetUser.mockResolvedValue({
                    email: "test@test.org",
                }),
            },
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        //@ts-ignore
                        organization: ["DHPrimeAdmins"],
                    },
                } as AccessTokenWithRSClaims,
            },
        });
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            activeMembership: {
                memberType: MemberType.PRIME_ADMIN,
                parsedName: "PrimeAdmins",
            },
            isAdminStrictCheck: true,
            dispatch: () => {},
            initialized: true,
            isUserAdmin: true,
            isUserReceiver: false,
            isUserSender: false,
            environment: "test",
        } as RSSessionContext);
        renderApp(<ReportStreamHeader>Test</ReportStreamHeader>);
        expect(screen.getByText(FeatureName.ADMIN)).toBeInTheDocument();
        expect(screen.getByText(FeatureName.DAILY_DATA)).toBeInTheDocument();
        expect(screen.getByText(FeatureName.UPLOAD)).toBeInTheDocument();
        expect(screen.getByText(FeatureName.SUBMISSIONS)).toBeInTheDocument();
    });

    test("senders see sender items and not receiver items", async () => {
        mockAuth.mockReturnValue({
            //@ts-ignore
            oktaAuth: {
                getUser: mockGetUser.mockResolvedValue({
                    email: "test@test.org",
                }),
            },
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        //@ts-ignore
                        organization: ["DHSender_ignore"],
                    },
                } as AccessTokenWithRSClaims,
            },
        });
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "ignore",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
            environment: "test",
        });
        renderApp(<ReportStreamHeader>Test</ReportStreamHeader>);
        expect(
            screen.queryByText(FeatureName.DAILY_DATA),
        ).not.toBeInTheDocument();
        expect(screen.getByText(FeatureName.UPLOAD)).toBeInTheDocument();
        expect(screen.getByText(FeatureName.SUBMISSIONS)).toBeInTheDocument();
    });

    test("receivers see receiver items and not sender items", async () => {
        mockAuth.mockReturnValue({
            //@ts-ignore
            oktaAuth: {
                getUser: mockGetUser.mockResolvedValue({
                    email: "test@test.org",
                }),
            },
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        //@ts-ignore
                        organization: ["DHignore"],
                    },
                } as AccessTokenWithRSClaims,
            },
        });
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "ignore",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: true,
            isUserSender: false,
            environment: "test",
        });
        renderApp(<ReportStreamHeader>Test</ReportStreamHeader>);
        expect(screen.getByText(FeatureName.DAILY_DATA)).toBeInTheDocument();
        expect(screen.queryByText(FeatureName.UPLOAD)).not.toBeInTheDocument();
        expect(
            screen.queryByText(FeatureName.SUBMISSIONS),
        ).not.toBeInTheDocument();
    });
});
