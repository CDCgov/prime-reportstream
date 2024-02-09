const m =
    await vi.importActual<typeof import("../oktaConfig")>("../oktaConfig");
const exports = {
    ...m,
    oktaAuthConfig: {
        ...m.oktaAuthConfig,
        tokenManager: { ...m.oktaAuthConfig.tokenManager, storage: "memory" },
    },
};

export default exports;
