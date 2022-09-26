import { render, screen } from "@testing-library/react";

import { MemberType } from "../hooks/UseOktaMemberships";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";
import { mockFeatureFlagContext } from "../contexts/__mocks__/FeatureFlagContext";
import { FeatureFlagName } from "../pages/misc/FeatureFlags";
import { mockTokenFromStorage } from "../utils/__mocks__/SessionStorageTools";

import { AuthElement } from "./AuthElement";

const mockUseNavigate = jest.fn();

jest.mock("react-router", () => ({
    useNavigate: () => mockUseNavigate,
}));

const TestElement = () => <h1>Test Passed</h1>;
const TestElementWithProp = (props: { test: string }) => <h1>{props.test}</h1>;

let mockCheckFlag = jest.fn();

describe("AuthElement unit tests", () => {
    beforeEach(() => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            checkFlag: () => true,
            featureFlags: [],
        });
    });
    test("Renders component when all checks pass", () => {
        mockCheckFlag = jest.fn((flag) => flag === FeatureFlagName.FOR_TEST);
        mockTokenFromStorage.mockReturnValueOnce("test token");
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.PRIME_ADMIN,
                parsedName: "PrimeAdmins",
            },
            dispatch: () => {},
            initialized: true,
        });
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            checkFlag: mockCheckFlag,
            featureFlags: [],
        });
        render(
            <AuthElement
                element={<TestElementWithProp test={"Success!"} />}
                requiredFeatureFlag={FeatureFlagName.FOR_TEST}
                requiredUserType={MemberType.PRIME_ADMIN}
            />
        );
        expect(screen.getByText("Success!")).toBeInTheDocument();
        expect(mockUseNavigate).not.toHaveBeenCalled();
    });
    test("Redirects when user not logged in (no token, no membership)", () => {
        mockTokenFromStorage.mockReturnValueOnce(undefined);
        mockSessionContext.mockReturnValueOnce({
            oktaToken: undefined,
            activeMembership: undefined,
            dispatch: () => {},
            initialized: true,
        });
        render(
            <AuthElement
                element={<TestElement />}
                requiredUserType={MemberType.RECEIVER}
            />
        );
        expect(mockUseNavigate).toHaveBeenCalledWith("/login");
    });
    test("Does not redirect when user refreshes app (token loads after membership)", () => {
        mockTokenFromStorage.mockReturnValueOnce("test token");
        mockSessionContext.mockReturnValueOnce({
            oktaToken: undefined,
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "all-in-one-health-ca",
            },
            dispatch: () => {},
            initialized: true,
        });
        render(
            <AuthElement
                element={<TestElement />}
                requiredUserType={MemberType.SENDER}
            />
        );
        expect(mockUseNavigate).not.toHaveBeenCalledWith();
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
    });
    test("Redirects when user is unauthorized user type", () => {
        mockTokenFromStorage.mockReturnValueOnce("test token");
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "all-in-one-health-ca",
            },
            dispatch: () => {},
            initialized: true,
        });
        render(
            <AuthElement
                element={<TestElement />}
                requiredUserType={MemberType.RECEIVER}
            />
        );
        expect(mockUseNavigate).not.toHaveBeenCalledWith("/login"); // Make sure first navigate() doesn't accidentally run
        expect(mockUseNavigate).toHaveBeenCalledWith("/");
    });
    test("Redirects when user lacks feature flag", () => {
        mockCheckFlag = jest.fn((flag) => flag !== FeatureFlagName.FOR_TEST);
        mockTokenFromStorage.mockReturnValueOnce("test token");
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "all-in-one-health-ca",
            },
            dispatch: () => {},
            initialized: true,
        });
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            checkFlag: mockCheckFlag,
            featureFlags: [],
        });
        render(
            <AuthElement
                element={<TestElement />}
                requiredFeatureFlag={FeatureFlagName.FOR_TEST}
            />
        );
        expect(mockUseNavigate).toHaveBeenCalledWith("/");
    });
    test("Considers all given authorized user types (affirmative)", () => {
        mockTokenFromStorage.mockReturnValueOnce("test token");
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "PrimeAdmins",
            },
            dispatch: () => {},
            initialized: true,
        });
        render(
            <AuthElement
                element={<TestElement />}
                requiredUserType={[MemberType.SENDER, MemberType.RECEIVER]}
            />
        );
        expect(screen.getByText("Test Passed")).toBeInTheDocument();
        expect(mockUseNavigate).not.toHaveBeenCalled();
    });
    test("Considers all given authorized user types (negative)", () => {
        mockTokenFromStorage.mockReturnValueOnce("test token");
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.NON_STAND,
                parsedName: "PrimeAdmins",
            },
            dispatch: () => {},
            initialized: true,
        });
        render(
            <AuthElement
                element={<TestElement />}
                requiredUserType={[MemberType.SENDER, MemberType.RECEIVER]}
            />
        );
        expect(mockUseNavigate).toHaveBeenCalledWith("/");
    });
});
