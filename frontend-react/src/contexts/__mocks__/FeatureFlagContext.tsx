import * as FeatureFlagModule from "../FeatureFlag";

export const mockFeatureFlagContext = jest.spyOn(
    FeatureFlagModule,
    "useFeatureFlags",
);
