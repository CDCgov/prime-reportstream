import * as FeatureFlagModule from "../FeatureFlagContext";

export const mockFeatureFlagContext = vi.spyOn(
    FeatureFlagModule,
    "useFeatureFlags",
);
