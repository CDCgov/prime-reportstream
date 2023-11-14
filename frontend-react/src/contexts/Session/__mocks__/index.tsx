import type { PartialDeep } from "type-fest";

import { mockRsconsole } from "../../../utils/console/__mocks__";
import type { SessionCtx } from "..";

export const defaultCtx = {
    authState: {
        isAuthenticated: false,
    },
    config: {
        AI_CONSOLE_SEVERITY_LEVELS: {
            assert: 0,
            debug: 0,
            error: 0,
            info: 0,
            trace: 0,
            warn: 0,
        },
    },
    logout: vi.fn(),
    oktaAuth: {},
    setActiveMembership: vi.fn(),
    site: {},
    user: {
        isAdminStrictCheck: false,
        isUserAdmin: false,
        isUserReceiver: false,
        isUserSender: false,
        isUserTransceiver: false,
    },
    rsconsole: mockRsconsole,
    activeMembership: {},
} satisfies PartialDeep<SessionCtx>;

const mod = await vi.importActual<typeof import("..")>("../");

const expt = {
    ...mod,
    useSessionContext: vi.fn(() => defaultCtx),
};

module.exports = expt;
