import type { FeatureFlagCtx } from "../FeatureFlagContext";

export const defaultCtx: FeatureFlagCtx = {
    checkFlags: vi.fn(() => false),
    dispatch: vi.fn(),
    featureFlags: [],
};
export const useFeatureFlags = vi.fn<any, FeatureFlagCtx>(() => defaultCtx);
export const {
    FeatureFlagActionType,
    FeatureFlagProvider,
    featureFlagReducer,
} = await vi.importActual<typeof import("../FeatureFlagContext")>(
    "../FeatureFlagContext",
);
