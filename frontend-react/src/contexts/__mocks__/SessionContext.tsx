import { OKTA_AUTH } from "../../oktaConfig";
import { mockRsconsole } from "../../utils/console/__mocks__/console";
import * as SessionContextModule from "../SessionContext";

export const mockSessionContext = jest.spyOn(
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
        rsconsole: mockRsconsole as any,
        ...impl,
    });
}
