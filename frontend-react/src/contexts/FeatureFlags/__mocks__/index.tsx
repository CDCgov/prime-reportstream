import type { PartialDeep } from "type-fest";

import type { FeatureFlagCtx } from "..";

export const defaultCtx = {
    checkFlags: vi.fn(() => false),
    dispatch: vi.fn(),
    featureFlags: [],
} satisfies PartialDeep<FeatureFlagCtx>;

const FeatureFlagModule = await vi.importActual("../");

module.exports = {
    ...FeatureFlagModule,
    useFeatureFlags: vi.fn(() => defaultCtx),
};
