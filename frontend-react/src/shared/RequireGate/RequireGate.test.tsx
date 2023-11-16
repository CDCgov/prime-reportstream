import { screen } from "@testing-library/react";

import { FeatureFlagName } from "../../pages/misc/FeatureFlags";
import { PERMISSIONS } from "../../utils/UsefulTypes";
import { useFeatureFlags } from "../../contexts/FeatureFlags";
import { render } from "../../utils/Test/render";

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
                auth={PERMISSIONS.PRIME_ADMIN}
            >
                <TestElementWithProp test={"Success!"} />
            </RequireGateBase>,
            {
                providers: {
                    Session: {
                        authState: {
                            isAuthenticated: true,
                            accessToken: {
                                claims: {
                                    organization: ["DHPrimeAdmins"],
                                },
                            },
                        } as any,
                    },
                },
            },
        );
        expect(screen.getByText("Success!")).toBeInTheDocument();
        expect(mockUseNavigate).not.toHaveBeenCalled();
    });
    test("Redirects when user not logged in (no token, no membership)", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={PERMISSIONS.RECEIVER}
            >
                <TestElement />
            </RequireGateBase>,
            {
                providers: {
                    Session: {
                        authState: {
                            isAuthenticated: false,
                        } as any,
                    },
                },
            },
        );
        expect(screen.getByText("Anonymous")).toBeInTheDocument();
    });
    test("Fails when user is unauthorized user type", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={PERMISSIONS.RECEIVER}
            >
                <TestElement />
            </RequireGateBase>,
            {
                providers: {
                    Session: {
                        authState: {
                            isAuthenticated: true,
                            accessToken: {
                                claims: {
                                    organization: ["DHSender_tx_phd"],
                                },
                            },
                        } as any,
                    },
                },
            },
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
                auth={[PERMISSIONS.SENDER, PERMISSIONS.RECEIVER]}
            >
                <TestElement />
            </RequireGateBase>,
            {
                providers: {
                    Session: {
                        authState: {
                            isAuthenticated: true,
                            accessToken: {
                                claims: {
                                    organization: ["DHSender_tx_phd"],
                                },
                            },
                        } as any,
                    },
                },
            },
        );
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
    });
    test("Considers all given authorized user types (negative)", () => {
        render(
            <RequireGateBase
                anonymousElement={<Anonymous />}
                failElement={<Fail />}
                auth={[PERMISSIONS.SENDER, PERMISSIONS.RECEIVER]}
            >
                <TestElement />
            </RequireGateBase>,
            {
                providers: {
                    Session: {
                        authState: {
                            isAuthenticated: true,
                            accessToken: {
                                claims: {
                                    organization: ["DHOther"],
                                },
                            },
                        } as any,
                    },
                },
            },
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
                auth={PERMISSIONS.PRIME_ADMIN}
            >
                <TestElement />
            </RequireGateBase>,
            {
                providers: {
                    Session: {
                        authState: {
                            isAuthenticated: true,
                            accessToken: {
                                claims: {
                                    organization: [
                                        "DHSender_tx_phd",
                                        "DHPrimeAdmins",
                                    ],
                                },
                            },
                        } as any,
                    },
                },
            },
        );
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
    });
});
