export const mockUseOktaAuth = jest.fn();

jest.mock("@okta/okta-react", () => ({
    useOktaAuth: mockUseOktaAuth,
}));
