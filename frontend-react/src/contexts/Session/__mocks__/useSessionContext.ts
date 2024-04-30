import type { RSSessionContext } from "../SessionProvider";
import { contextFixture } from "../useSessionContext.fixtures";

const useSessionContext = vi.fn(() => contextFixture);

export function mockSessionContentReturnValue(
    impl?: Partial<RSSessionContext>,
): () => RSSessionContext {
    return useSessionContext.mockReturnValue({
        ...(contextFixture as any),
        ...impl,
    });
}

export default useSessionContext;
