import {
    AxiosOptionsWithSegments,
    RSEndpoint,
} from "../../../config/endpoints";
import {
    type RSSessionContext,
    staticAuthorizedFetch,
} from "../SessionProvider";
import { configFixture, contextFixture } from "../useSessionContext.fixtures";

const useSessionContext = vi.fn(() => contextFixture);

export function mockSessionContentReturnValue(
    impl?: Partial<RSSessionContext>,
): () => RSSessionContext {
    const newValue = {
        ...(contextFixture as any),
        ...impl,
    };

    if (!impl?.authorizedFetch) {
        newValue.authorizedFetch = (
            options: Partial<AxiosOptionsWithSegments>,
            endpointConfig?: RSEndpoint,
        ) => {
            return staticAuthorizedFetch({
                apiUrl: configFixture.API_ROOT,
                options,
                endpointConfig,
                accessToken: newValue.authState.accessToken?.accessToken,
                organization: newValue.activeMembership?.parsedName,
                sessionId: "",
            });
        };
    }

    return useSessionContext.mockReturnValue(newValue);
}

export default useSessionContext;
