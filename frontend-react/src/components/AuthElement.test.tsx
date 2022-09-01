import { render } from "@testing-library/react";

import { MemberType } from "../hooks/UseOktaMemberships";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";
import * as Flags from "../pages/misc/FeatureFlags";

import { AuthElement } from "./AuthElement";

const mockUseNavigate = jest.fn();
const mockCheckFeatureFlag = jest.spyOn(Flags, "CheckFeatureFlag");
const { FeatureFlagName } = Flags;
jest.mock("react-router", () => ({
    useNavigate: () => mockUseNavigate,
}));

const TestElement = () => <h1>Test Passed</h1>;

describe("AuthElement unit tests", () => {
    test("Redirects when user not logged in", () => {
        mockSessionContext.mockReturnValueOnce({
            oktaToken: undefined,
            activeMembership: undefined,
            dispatch: () => {},
        });
        render(
            <AuthElement
                element={TestElement}
                requiredUserType={MemberType.RECEIVER}
            />
        );
        expect(mockUseNavigate).toHaveBeenCalledWith("/login");
    });
    test("Redirects when user is unauthorized user type", () => {
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "all-in-one-health-ca",
            },
            dispatch: () => {},
        });
        render(
            <AuthElement
                element={TestElement}
                requiredUserType={MemberType.RECEIVER}
            />
        );
        expect(mockUseNavigate).toHaveBeenCalledWith("/");
    });
    test("Redirects when non-admin user lacks feature flag", () => {
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "all-in-one-health-ca",
            },
            dispatch: () => {},
        });
        mockCheckFeatureFlag.mockImplementation((arg: string) => {
            return arg !== FeatureFlagName.FOR_TEST;
        });
        render(
            <AuthElement
                element={TestElement}
                requiredFeatureFlag={FeatureFlagName.FOR_TEST}
            />
        );
        expect(mockUseNavigate).toHaveBeenCalledWith("/");
    });
    test("Redirects when admin lacks feature flag", () => {
        mockSessionContext.mockReturnValueOnce({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.PRIME_ADMIN,
                parsedName: "PrimeAdmins",
            },
            dispatch: () => {},
        });
        mockCheckFeatureFlag.mockImplementation((arg: string) => {
            return arg !== FeatureFlagName.FOR_TEST;
        });
        render(
            <AuthElement
                element={TestElement}
                requiredFeatureFlag={FeatureFlagName.FOR_TEST}
            />
        );
        expect(mockCheckFeatureFlag).toHaveBeenCalledWith(
            FeatureFlagName.FOR_TEST
        );
        expect(mockUseNavigate).toHaveBeenCalledWith("/admin/features");
    });
});
