import { screen } from "@testing-library/react";

import { MemberType } from "../../utils/OrganizationUtils";
import { render } from "../../utils/Test/render";

import { useSessionContext } from ".";

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
        render(<TestComponent />, {
            providers: {
                Session: {
                    activeMembership: {
                        parsedName: "PrimeAdmins",
                        memberType: MemberType.PRIME_ADMIN,
                    },
                },
            },
        });
        expect(screen.getByText("passed")).toBeInTheDocument();
    });
    test("admin hard check is false when user is not admin member type", () => {
        render(<TestComponent />, {
            providers: {
                Session: {
                    activeMembership: {
                        parsedName: "testOrg",
                        memberType: MemberType.SENDER,
                    },
                },
            },
        });
        expect(screen.getByText("failed")).toBeInTheDocument();
    });
});
