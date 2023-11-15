import {
    mockGetSavedFeatureFlags,
    mockStoreFeatureFlags,
} from "../../utils/__mocks__/SessionStorageTools";

import {
    featureFlagReducer,
    useFeatureFlags,
    FeatureFlagActionType,
    FeatureFlagProvider,
} from ".";

vi.mock("../../config", async () => {
    const originalModule =
        await vi.importActual<typeof import("../../config")>("../../config");
    return {
        ...originalModule,
        default: {
            ...originalModule.default,
            DEFAULT_FEATURE_FLAGS: ["flag-3"],
        },
    };
});

vi.unmock("./");

const providerValueMonitor = vi.fn();

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
            { type: FeatureFlagActionType.ADD, payload: "NEW-flag  " },
        );

        expect(result).toEqual({ featureFlags: ["new-flag"] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith(["new-flag"]);
    });

    test("does not duplciate feature flag if already present", () => {
        const result = featureFlagReducer(
            { featureFlags: ["new-flag"] },
            { type: FeatureFlagActionType.ADD, payload: "new-FLAG    " },
        );

        expect(result).toEqual({ featureFlags: ["new-flag"] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith(["new-flag"]);
    });

    test("removes feature flag correctly", () => {
        const result = featureFlagReducer(
            { featureFlags: ["new-flag"] },
            { type: FeatureFlagActionType.REMOVE, payload: "new-FLAG    " },
        );

        expect(result).toEqual({ featureFlags: [] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith([]);
    });

    test("does nothing if trying to remove a flag that is not present", () => {
        const result = featureFlagReducer(
            { featureFlags: ["new-flag"] },
            { type: FeatureFlagActionType.REMOVE, payload: "old-flag" },
        );

        expect(result).toEqual({ featureFlags: ["new-flag"] });
        expect(mockStoreFeatureFlags).toHaveBeenCalledWith(["new-flag"]);
    });
});

describe.skip("FeatureFlagProvider", () => {
    const mockSavedFlags = ["flag-1", "flag-2"];
    function setup() {
        mockGetSavedFeatureFlags.mockReturnValue(mockSavedFlags);
        render(<FeatureFlagProviderTestRenderer />);
    }
    test("provides initial state with saved flags and env level flags", async () => {
        setup();
        expect(providerValueMonitor).toHaveBeenCalledTimes(1);

        const { featureFlags } = providerValueMonitor.mock.lastCall[0];
        expect(featureFlags).toEqual(["flag-1", "flag-2", "flag-3"]);
    });
    test("provides flagCheck that correctly checks against all flags", () => {
        setup();
        const { checkFlags } = providerValueMonitor.mock.lastCall[0];
        expect(checkFlags("flag-1")).toEqual(true);
        expect(checkFlags("flag-3")).toEqual(true);
        expect(checkFlags("flag-4")).toEqual(false);
    });
});
