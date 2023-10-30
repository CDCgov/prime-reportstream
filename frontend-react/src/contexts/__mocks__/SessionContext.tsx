import { OKTA_AUTH } from "../../oktaConfig";
import * as SessionContextModule from "../SessionContext";

export const mockSessionContext = vi.spyOn(
    SessionContextModule,
    "useSessionContext",
);

export function mockSessionContentReturnValue(
    impl?: Partial<SessionContextModule.RSSessionContext>,
) {
    return mockSessionContext.mockReturnValue({
        oktaAuth: OKTA_AUTH,
        authState: {},
        logout: () => void 0,
        user: {
            isUserAdmin: false,
            isUserSender: false,
            isUserReceiver: false,
        } as any,
        setActiveMembership: () => void 0,
        config: {} as any,
        site: {} as any,
        ...impl,
    });
}
