import { render } from "@testing-library/react";

import {
    mockGetSavedFeatureFlags,
    mockStoreFeatureFlags,
} from "../utils/__mocks__/SessionStorageTools";

import {
    featureFlagReducer,
    useFeatureFlags,
    FeatureFlagActionType,
    FeatureFlagProvider,
} from "./FeatureFlagContext";

jest.mock("../config", () => {
    return {
        default: {
            DEFAULT_FEATURE_FLAGS: ["flag-3"],
        },
        __esModule: true,
    };
});

const providerValueMonitor = jest.fn();

const DummyFeatureFlagConsumer = () => {
    const values = useFeatureFlags();
    return <>{providerValueMonitor(values)}</>;
};

const FeatureFlagProviderTestRenderer = () => {
    return (
        <FeatureFlagProvider>
            <DummyFeatureFlagConsumer />
        </FeatureFlagProvider>
    );
};

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

describe("FeatureFlagProvider", () => {
    const mockSavedFlags = ["flag-1", "flag-2"];
    beforeEach(() => {
        mockGetSavedFeatureFlags.mockReturnValue(mockSavedFlags);
        render(<FeatureFlagProviderTestRenderer />);
    });
    test("provides initial state with saved flags and env level flags", async () => {
        expect(providerValueMonitor).toHaveBeenCalledTimes(1);

        const { featureFlags } = providerValueMonitor.mock.lastCall[0];
        expect(featureFlags).toEqual(["flag-1", "flag-2", "flag-3"]);
    });
    test("provides flagCheck that correctly checks against all flags", () => {
        const { checkFlag } = providerValueMonitor.mock.lastCall[0];
        expect(checkFlag("flag-1")).toEqual(true);
        expect(checkFlag("flag-3")).toEqual(true);
        expect(checkFlag("flag-4")).toEqual(false);
    });
});
