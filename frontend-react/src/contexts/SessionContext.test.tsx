import { screen } from "@testing-library/react";

import { MemberType } from "../hooks/UseOktaMemberships";
import { mockUseOktaMemberships } from "../hooks/__mocks__/UseOktaMemberships";
import { renderWithSession } from "../utils/CustomRenderUtils";

import { useSessionContext } from "./SessionContext";

describe("SessionContext admin hard check", () => {
    /* Because the session has to be consumed within the session wrapper, I couldn't use renderHook() to
     * get back a returned state value -- the provider itself needs to be accessed from within a component for
     * any provider logic (i.e. adminHardCheck) to be executed. Otherwise, you're just rendering the default
     * Context, which sets everything to undefined, null, or empty. */
    const TestComponent = () => {
        const { isAdminStrictCheck } = useSessionContext();
        // Conditions to fail
        if (!isAdminStrictCheck) return <>failed</>;
        return <>passed</>;
    };
    test("admin hard check is true when user has admin org in memberships map", async () => {
        mockUseOktaMemberships.mockReturnValue({
            state: {
                activeMembership: {
                    parsedName: "testOrg",
                    memberType: MemberType.SENDER,
                },
                memberships: new Map().set("DHPrimeAdmins", {
                    parsedName: "PrimeAdmins",
                    memberType: MemberType.PRIME_ADMIN,
                }),
                initialized: true,
            },
            dispatch: () => {},
        });
        renderWithSession(<TestComponent />);
        expect(screen.getByText("passed")).toBeInTheDocument();
    });
    test("admin hard check is false when user has has no trace of admin group", async () => {
        mockUseOktaMemberships.mockReturnValue({
            state: {
                activeMembership: {
                    parsedName: "testOrg",
                    memberType: MemberType.SENDER,
                },
                memberships: new Map(),
                initialized: true,
            },
            dispatch: () => {},
        });
        renderWithSession(<TestComponent />);
        expect(screen.getByText("failed")).toBeInTheDocument();
    });
    /* Extra safety. This case _shouldn't_ happen unless we absolutely destroy our useOktaMemberships hook! */
    test("admin hard check is false when user's memberships DON'T contain admin, but activeMembership IS admin", async () => {
        mockUseOktaMemberships.mockReturnValue({
            state: {
                activeMembership: {
                    parsedName: "PrimeAdmins",
                    memberType: MemberType.PRIME_ADMIN,
                },
                memberships: new Map(),
                initialized: true,
            },
            dispatch: () => {},
        });
        renderWithSession(<TestComponent />);
        expect(screen.getByText("failed")).toBeInTheDocument();
    });
});
