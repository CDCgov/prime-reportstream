import * as FeatureFlagModule from "../FeatureFlag";

export const mockFeatureFlagContext = vi.spyOn(
    FeatureFlagModule,
    "useFeatureFlags",
);
