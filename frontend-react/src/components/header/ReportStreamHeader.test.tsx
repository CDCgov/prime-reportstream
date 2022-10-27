import * as OktaReact from "@okta/okta-react";
import { screen } from "@testing-library/react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import { renderWithSession } from "../../utils/CustomRenderUtils";
import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { RSSessionContext } from "../../contexts/SessionContext";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AccessTokenWithRSClaims } from "../../utils/OrganizationUtils";

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
        renderWithSession(<ReportStreamHeader />);
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
        } as RSSessionContext);
        renderWithSession(<ReportStreamHeader />);
        expect(screen.getByText("Admin")).toBeInTheDocument();
        expect(screen.getByText("Daily data")).toBeInTheDocument();
        expect(screen.getByText("Upload")).toBeInTheDocument();
        expect(screen.getByText("Submissions")).toBeInTheDocument();
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
        });
        renderWithSession(<ReportStreamHeader />);
        expect(screen.queryByText("Daily data")).not.toBeInTheDocument();
        expect(screen.getByText("Upload")).toBeInTheDocument();
        expect(screen.getByText("Submissions")).toBeInTheDocument();
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
        });
        renderWithSession(<ReportStreamHeader />);
        expect(screen.getByText("Daily data")).toBeInTheDocument();
        expect(screen.queryByText("Upload")).not.toBeInTheDocument();
        expect(screen.queryByText("Submissions")).not.toBeInTheDocument();
    });
});
