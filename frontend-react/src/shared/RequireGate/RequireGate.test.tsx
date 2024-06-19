import { screen } from "@testing-library/react";

import { RequireGateBase } from "./RequireGate";
import useFeatureFlags from "../../contexts/FeatureFlag/useFeatureFlags";
import { FeatureFlagName } from "../../pages/misc/FeatureFlags";
import { renderApp } from "../../utils/CustomRenderUtils";
import { PERMISSIONS } from "../../utils/UsefulTypes";

vi.mock("../../contexts/FeatureFlag/useFeatureFlags");

const mockFeatureFlagContext = vi.mocked(useFeatureFlags);
const mockUseNavigate = vi.fn();
const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../contexts/Session/__mocks__/useSessionContext")
>("../../contexts/Session/useSessionContext");

vi.mock("react-router", async (importActual) => ({
    ...(await importActual<typeof import("react-router")>()),
    useNavigate: () => mockUseNavigate,
}));

const TestElement = () => <h1>Test Passed</h1>;
const TestElementWithProp = (props: { test: string }) => <h1>{props.test}</h1>;

let mockCheckFlags = vi.fn();

const Anonymous = () => <>Anonymous</>;
const Fail = () => <>Failure</>;

describe("RequireGate", () => {
    beforeEach(() => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => void 0,
            checkFlags: () => true,
            featureFlags: [],
        });
    });
    test("Renders component when all checks pass", () => {
        mockCheckFlags = vi.fn((flag) => flag === FeatureFlagName.FOR_TEST);
        mockSessionContentReturnValue({
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        organization: ["DHPrimeAdmins"],
                    },
                },
            } as any,
        });
        mockFeatureFlagContext.mockReturnValue({
            checkFlags: mockCheckFlags,
        } as any);
        renderApp(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                featureFlags={FeatureFlagName.FOR_TEST}
                auth={PERMISSIONS.PRIME_ADMIN}
            >
                <TestElementWithProp test={"Success!"} />
            </RequireGateBase>,
        );
        expect(screen.getByText("Success!")).toBeInTheDocument();
        expect(mockUseNavigate).not.toHaveBeenCalled();
    });
    test("Redirects when user not logged in (no token, no membership)", () => {
        mockSessionContentReturnValue({
            authState: {
                isAuthenticated: false,
            } as any,
        });
        renderApp(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={PERMISSIONS.RECEIVER}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Anonymous")).toBeInTheDocument();
    });
    test("Fails when user is unauthorized user type", () => {
        mockSessionContentReturnValue({
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        organization: ["DHSender_tx_phd"],
                    },
                },
            } as any,
        });
        renderApp(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={PERMISSIONS.RECEIVER}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Failure")).toBeInTheDocument();
    });
    test("Fails when user lacks feature flag", () => {
        mockCheckFlags = vi.fn(() => false);
        mockSessionContentReturnValue({});
        mockFeatureFlagContext.mockReturnValue({
            checkFlags: mockCheckFlags,
        } as any);
        renderApp(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                featureFlags={FeatureFlagName.FOR_TEST}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Failure")).toBeInTheDocument();
    });
    test("Considers all given authorized user types (affirmative)", () => {
        mockSessionContentReturnValue({
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        organization: ["DHSender_tx_phd"],
                    },
                },
            } as any,
        });
        renderApp(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={[PERMISSIONS.SENDER, PERMISSIONS.RECEIVER]}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
    });
    test("Considers all given authorized user types (negative)", () => {
        mockSessionContentReturnValue({
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        organization: ["DHOther"],
                    },
                },
            } as any,
        });
        renderApp(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={[PERMISSIONS.SENDER, PERMISSIONS.RECEIVER]}
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
        mockSessionContentReturnValue({
            authState: {
                isAuthenticated: true,
                accessToken: {
                    claims: {
                        organization: ["DHSender_tx_phd", "DHPrimeAdmins"],
                    },
                },
            } as any,
        });
        renderApp(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={PERMISSIONS.PRIME_ADMIN}
            >
                <TestElement />
            </RequireGateBase>,
        );
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
    });
});
