import type { SessionCtx } from "../SessionContext";

export const defaultCtx: SessionCtx = {
    authState: {
        isAuthenticated: false,
    },
    config: {} as any,
    logout: vi.fn(),
    oktaAuth: {} as any,
    setActiveMembership: vi.fn(),
    site: {} as any,
    user: {
        isAdminStrictCheck: false,
        isUserAdmin: false,
        isUserReceiver: false,
        isUserSender: false,
        isUserTransceiver: false,
    },
};
export const useSessionContext = vi.fn(() => defaultCtx);
export const {
    SessionContext,
    SessionProviderBase,
    default: dft,
} = await vi.importActual<typeof import("../SessionContext")>(
    "../SessionContext",
);
export default dft;
