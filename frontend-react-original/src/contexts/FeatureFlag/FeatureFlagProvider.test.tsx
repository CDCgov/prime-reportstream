import FeatureFlagProvider, { FeatureFlagActionType, featureFlagReducer } from "./FeatureFlagProvider";
import useFeatureFlags from "./useFeatureFlags";
import { mockGetSavedFeatureFlags, mockStoreFeatureFlags } from "../../utils/__mocks__/SessionStorageTools";
import { renderApp } from "../../utils/CustomRenderUtils";

vi.unmock("./useFeatureFlags");

vi.mock("../../config", async (importActual) => {
    const originalModule = await importActual<typeof import("../../config")>();
    return {
        ...originalModule,
        default: {
            ...originalModule.default,
            DEFAULT_FEATURE_FLAGS: ["flag-3"],
        },
        __esModule: true,
    };
});

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

    test("does not duplicate feature flag if already present", () => {
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

describe("FeatureFlagProvider", () => {
    const mockSavedFlags = ["flag-1", "flag-2"];
    function setup() {
        mockGetSavedFeatureFlags.mockReturnValue(mockSavedFlags);
        renderApp(<FeatureFlagProviderTestRenderer />);
    }
    test("provides initial state with saved flags and env level flags", () => {
        setup();
        expect(providerValueMonitor).toHaveBeenCalledTimes(1);

        const { featureFlags } = providerValueMonitor.mock.lastCall?.[0] ?? {};
        expect(featureFlags).toEqual(["flag-1", "flag-2", "flag-3"]);
    });
    test("provides flagCheck that correctly checks against all flags", () => {
        setup();
        const { checkFlags } = providerValueMonitor.mock.lastCall?.[0] ?? {};
        expect(checkFlags("flag-1")).toEqual(true);
        expect(checkFlags("flag-3")).toEqual(true);
        expect(checkFlags("flag-4")).toEqual(false);
    });
});
