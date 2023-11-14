const OktaReactModule = await vi.importActual("@okta/okta-react");

module.exports = {
    ...OktaReactModule,
};

export const useOkta = vi.fn(() => ({
    authState: {},
    oktaAuth: {},
}));
