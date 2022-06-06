import * as OktaReact from "@okta/okta-react";
import { screen } from "@testing-library/react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import { renderWithSession } from "../../utils/CustomRenderUtils";
import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { ISessionContext } from "../../contexts/SessionContext";
import { MemberType } from "../../hooks/UseOktaMemberships";

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
        mockSessionContext.mockReturnValue({} as ISessionContext);
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
                },
            },
        });
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.PRIME_ADMIN,
                        parsedName: "PrimeAdmins",
                    },
                },
            },
        });
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
                },
            },
        });
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.SENDER,
                        parsedName: "ignore",
                    },
                },
            },
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
                },
            },
        });
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "ignore",
                    },
                },
            },
        });
        renderWithSession(<ReportStreamHeader />);
        expect(screen.getByText("Daily data")).toBeInTheDocument();
        expect(screen.queryByText("Upload")).not.toBeInTheDocument();
        expect(screen.queryByText("Submissions")).not.toBeInTheDocument();
    });
});
