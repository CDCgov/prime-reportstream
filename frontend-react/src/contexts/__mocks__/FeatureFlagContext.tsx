import * as FeatureFlagModule from "../FeatureFlagContext";

export const mockFeatureFlagContext = jest.spyOn(
    FeatureFlagModule,
    "useFeatureFlags",
);
