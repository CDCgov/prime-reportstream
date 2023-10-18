import * as SessionContextModule from "../SessionContext";

export const mockSessionContext = jest.spyOn(
    SessionContextModule,
    "useSessionContext",
);

export function mockSessionContentReturnValue(
    impl: Partial<SessionContextModule.RSSessionContext>,
) {
    return mockSessionContext.mockReturnValue({
        logout: () => void 0,
        isUserAdmin: false,
        isUserSender: false,
        isUserReceiver: false,
        environment: "test",
        setActiveMembership: () => void 0,
        ...impl,
    });
}
