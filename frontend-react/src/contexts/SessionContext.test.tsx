import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";
import { MemberType } from "../utils/OrganizationUtils";

import { useSessionContext } from "./SessionContext";
import { mockSessionContentReturnValue } from "./__mocks__/SessionContext";

describe("SessionContext admin hard check", () => {
    /* Because the session has to be consumed within the session wrapper, I couldn't use renderHook() to
     * get back a returned state value -- the provider itself needs to be accessed from within a component for
     * any provider logic (i.e. adminHardCheck) to be executed. Otherwise, you're just rendering the default
     * Context, which sets everything to undefined, null, or empty. */
    const TestComponent = () => {
        const { activeMembership = {} as any } = useSessionContext();
        // Conditions to fail
        if (activeMembership.memberType !== MemberType.PRIME_ADMIN)
            return <>failed</>;
        return <>passed</>;
    };
    test("admin hard check is true when user is admin member type", () => {
        mockSessionContentReturnValue({
            activeMembership: {
                parsedName: "PrimeAdmins",
                memberType: MemberType.PRIME_ADMIN,
            },
        });
        renderApp(<TestComponent />);
        expect(screen.getByText("passed")).toBeInTheDocument();
    });
    test("admin hard check is false when user is not admin member type", () => {
        mockSessionContentReturnValue({
            activeMembership: {
                parsedName: "testOrg",
                memberType: MemberType.SENDER,
            },
        });
        renderApp(<TestComponent />);
        expect(screen.getByText("failed")).toBeInTheDocument();
    });
});
