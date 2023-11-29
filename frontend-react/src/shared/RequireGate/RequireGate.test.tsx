import { screen } from "@testing-library/react";

import { FeatureFlagName } from "../../pages/misc/FeatureFlags";
import { useFeatureFlags } from "../../contexts/FeatureFlags";
import { render } from "../../utils/Test/render";
import { MemberType, RSUser } from "../../utils/OrganizationUtils";

import { RequireGateBase } from "./RequireGate";

const mockUseFeatureFlags = vi.mocked(useFeatureFlags);
const mockUseNavigate = vi.fn();

vi.mock("react-router", async () => ({
    ...(await vi.importActual<typeof import("react-router")>("react-router")),
    useNavigate: () => mockUseNavigate,
}));

const TestElement = () => <h1>Test Passed</h1>;
const TestElementWithProp = (props: { test: string }) => <h1>{props.test}</h1>;

let mockCheckFlags = vi.fn();

const Anonymous = () => <>Anonymous</>;
const Fail = () => <>Failure</>;

describe("RequireGate", () => {
    beforeEach(() => {
        mockUseFeatureFlags.mockReturnValue({
            dispatch: () => {},
            checkFlags: () => true,
            featureFlags: [],
        });
    });
    test("Renders component when all checks pass", () => {
        mockCheckFlags = vi.fn((flag) => flag === FeatureFlagName.FOR_TEST);
        mockUseFeatureFlags.mockReturnValue({
            checkFlags: mockCheckFlags,
        } as any);
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                featureFlags={FeatureFlagName.FOR_TEST}
                auth={MemberType.PRIME_ADMIN}
                user={
                    new RSUser({
                        claims: { organization: ["DHPrimeAdmins"] } as any,
                    })
                }
                checkFlags={mockCheckFlags}
            >
                <TestElementWithProp test={"Success!"} />
            </RequireGateBase>,
        );
        expect(screen.getByText("Success!")).toBeInTheDocument();
        expect(mockUseNavigate).not.toHaveBeenCalled();
    });
    test("Redirects when user not logged in (no token, no membership)", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={MemberType.RECEIVER}
                user={new RSUser()}
                checkFlags={mockCheckFlags}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Anonymous")).toBeInTheDocument();
    });
    test("Fails when user is unauthorized user type", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={MemberType.RECEIVER}
                user={
                    new RSUser({
                        claims: {
                            organization: ["DHSender_tx_phd"],
                        },
                    } as any)
                }
                checkFlags={mockCheckFlags}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Failure")).toBeInTheDocument();
    });
    test("Fails when user lacks feature flag", () => {
        mockCheckFlags = vi.fn(() => false);
        mockUseFeatureFlags.mockReturnValue({
            checkFlags: mockCheckFlags,
        } as any);
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                featureFlags={FeatureFlagName.FOR_TEST}
                user={new RSUser()}
                checkFlags={mockCheckFlags}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Failure")).toBeInTheDocument();
    });
    test("Considers all given authorized user types (affirmative)", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={[MemberType.SENDER, MemberType.RECEIVER]}
                user={
                    new RSUser({
                        claims: {
                            organization: ["DHSender_tx-phd", "DHtx-phd"],
                        },
                    } as any)
                }
                checkFlags={mockCheckFlags}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
    });
    test("Considers all given authorized user types (negative)", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={[MemberType.SENDER, MemberType.RECEIVER]}
                user={
                    new RSUser({
                        claims: {
                            organization: ["DHOther"],
                        },
                    } as any)
                }
                checkFlags={mockCheckFlags}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Failure")).toBeInTheDocument();
    });
    /* This can happen if a user is a member of multiple Okta orgs, and the one set for them
     * was a non-admin memberType. Because admins are the only ones able to mock memberType, we assign
     * it based on a users first Okta group.
     *
     * In this example, you can see what that session state would look like. */
    test("Permits admins whose active membership is not DHPrimeAdmins", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={MemberType.PRIME_ADMIN}
                user={
                    new RSUser({
                        claims: {
                            organization: ["DHSender_tx_phd", "DHPrimeAdmins"],
                        },
                    } as any)
                }
                checkFlags={mockCheckFlags}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
    });
});
