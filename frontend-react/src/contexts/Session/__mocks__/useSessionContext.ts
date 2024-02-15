import { SeverityLevel } from "@microsoft/applicationinsights-web";
import { mockRsconsole } from "../../../utils/rsConsole/rsConsole.fixtures";
import type { RSSessionContext } from "../SessionProvider";

const useSessionContext = jest.fn().mockReturnValue({
    oktaAuth: {} as any,
    authState: {},
    logout: () => void 0,
    user: {
        isUserAdmin: false,
        isUserSender: false,
        isUserReceiver: false,
    } as any,
    setActiveMembership: () => void 0,
    config: {
        AI_CONSOLE_SEVERITY_LEVELS: {
            info: SeverityLevel.Information,
            warn: SeverityLevel.Warning,
            error: SeverityLevel.Error,
            debug: SeverityLevel.Verbose,
            assert: SeverityLevel.Error,
            trace: SeverityLevel.Warning,
        },
    } as any,
    site: {} as any,
    rsConsole: mockRsconsole as any,
});

export function mockSessionContentReturnValue(
    impl?: Partial<RSSessionContext>,
) {
    return useSessionContext.mockReturnValue({
        oktaAuth: {} as any,
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
        rsConsole: mockRsconsole as any,
        ...impl,
    });
}

export default useSessionContext;
