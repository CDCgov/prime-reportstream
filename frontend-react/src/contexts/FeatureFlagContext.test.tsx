import { renderHook } from "@testing-library/react-hooks";

import {
    mockGetSavedFeatureFlags,
    mockStoreFeatureFlags,
} from "../utils/__mocks__/SessionStorageTools";
import {
    featureFlagReducer,
    useFeatureFlags,
    FeatureFlagActionType,
} from "./FeatureFlagContext";

describe("featureFlagReducer", () => {
    test("adds feature flag correctly", () => {
        const result = featureFlagReducer(
            { featureFlags: [] },
            { type: FeatureFlagActionType.ADD, payload: "NEW-flag  " }
        );

        expect(result).toEqual({ featureFlags: ["new-flag"] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith(["new-flag"]);
    });

    test("does not duplciate feature flag if already present", () => {
        const result = featureFlagReducer(
            { featureFlags: ["new-flag"] },
            { type: FeatureFlagActionType.ADD, payload: "new-FLAG    " }
        );

        expect(result).toEqual({ featureFlags: ["new-flag"] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith(["new-flag"]);
    });

    test("removes feature flag correctly", () => {
        const result = featureFlagReducer(
            { featureFlags: ["new-flag"] },
            { type: FeatureFlagActionType.REMOVE, payload: "new-FLAG    " }
        );

        expect(result).toEqual({ featureFlags: [] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith([]);
    });

    test("does nothing if trying to remove a flag that is not present", () => {
        const result = featureFlagReducer(
            { featureFlags: ["new-flag"] },
            { type: FeatureFlagActionType.REMOVE, payload: "old-flag" }
        );

        expect(result).toEqual({ featureFlags: ["new-flag"] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith(["new-flag"]);
    });
});

// test provider by way of context hook
describe("useFeatureFlags", () => {
    test("returns initial state with saved flags and env level flags", () => {});
});
