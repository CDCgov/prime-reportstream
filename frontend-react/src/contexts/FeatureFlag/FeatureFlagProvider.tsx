import { uniq } from "lodash";
import { createContext, Dispatch, PropsWithChildren, useCallback, useMemo, useReducer } from "react";

import config from "../../config";
import { getSavedFeatureFlags, storeFeatureFlags } from "../../utils/SessionStorageTools";

export enum FeatureFlagActionType {
    ADD = "ADD",
    REMOVE = "REMOVE",
}

export interface FeatureFlagAction {
    type: FeatureFlagActionType;
    payload: string;
}

export type StringCheck = (flags: string | string[]) => boolean;

export interface FeatureFlagContextValues {
    checkFlags: StringCheck;
    dispatch: Dispatch<FeatureFlagAction>;
    featureFlags: string[];
}

export interface FeatureFlagState {
    featureFlags: string[];
}

const { DEFAULT_FEATURE_FLAGS } = config;

export const FeatureFlagContext = createContext<FeatureFlagContextValues>({
    checkFlags: (flags: string | string[]) => {
        const arr = Array.isArray(flags) ? flags : [flags];
        return !!DEFAULT_FEATURE_FLAGS.find((el) => arr.includes(el));
    },
    dispatch: () => void 0,
    featureFlags: DEFAULT_FEATURE_FLAGS,
});

export const featureFlagReducer = (state: FeatureFlagState, action: FeatureFlagAction) => {
    const { type, payload } = action;
    let newFlags;
    switch (type) {
        case FeatureFlagActionType.ADD:
            newFlags = uniq(state.featureFlags.concat([payload.trim().toLowerCase()]));
            storeFeatureFlags(newFlags);
            return { featureFlags: newFlags };
        // we're hiding the delete button for env level flags, so
        // we shouldn't run into a case where this is hit for those
        case FeatureFlagActionType.REMOVE:
            newFlags = state.featureFlags.filter((f) => f !== payload.trim().toLowerCase());
            storeFeatureFlags(newFlags);
            return { featureFlags: newFlags };
        default:
            return state;
    }
};

const FeatureFlagProvider = ({ children }: PropsWithChildren<object>) => {
    // reducer manages per user feature flags only
    const [{ featureFlags }, dispatch] = useReducer(featureFlagReducer, {
        featureFlags: getSavedFeatureFlags(),
    });

    // makes sure default values from environment don't get stored in local storage.
    // if the env turns on the flag, we don't want a user accidentally accessing it later on
    const allFeatureFlags: string[] = useMemo(() => uniq(featureFlags.concat(DEFAULT_FEATURE_FLAGS)), [featureFlags]);

    const checkFlags = useCallback(
        (flags: string | string[]): boolean => {
            const arr = Array.isArray(flags) ? flags : [flags];
            return !!allFeatureFlags.find((el) => arr.includes(el));
        },
        [allFeatureFlags],
    );

    const ctx = useMemo(
        () => ({
            dispatch,
            checkFlags,
            featureFlags: allFeatureFlags,
        }),
        [allFeatureFlags, checkFlags],
    );

    return <FeatureFlagContext.Provider value={ctx}>{children}</FeatureFlagContext.Provider>;
};

export default FeatureFlagProvider;
