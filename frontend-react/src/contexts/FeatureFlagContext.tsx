import React, {
    createContext,
    useCallback,
    useContext,
    useReducer,
    useMemo,
} from "react";
import uniq from "lodash.uniq";

import config from "../config";
import {
    getSavedFeatureFlags,
    storeFeatureFlags,
} from "../utils/SessionStorageTools";

export enum FeatureFlagActionType {
    ADD = "ADD",
    REMOVE = "REMOVE",
}

export interface FeatureFlagAction {
    type: FeatureFlagActionType;
    payload: string;
}

type StringCheck = {
    (flag: string): boolean;
};

interface FeatureFlagContextValues {
    checkFlag: StringCheck;
    dispatch: React.Dispatch<FeatureFlagAction>;
    featureFlags: string[];
}

type FeatureFlagState = {
    featureFlags: string[];
};

const { DEFAULT_FEATURE_FLAGS } = config;

const FeatureFlagContext = createContext<FeatureFlagContextValues>({
    checkFlag: (flag: string) =>
        !!DEFAULT_FEATURE_FLAGS.find((el) => el === flag),
    dispatch: () => {},
    featureFlags: DEFAULT_FEATURE_FLAGS,
});

export const featureFlagReducer = (
    state: FeatureFlagState,
    action: FeatureFlagAction,
) => {
    const { type, payload } = action;
    let newFlags;
    switch (type) {
        case FeatureFlagActionType.ADD:
            newFlags = uniq(
                state.featureFlags.concat([payload.trim().toLowerCase()]),
            );
            storeFeatureFlags(newFlags);
            return { featureFlags: newFlags };
        // we're hiding the delete button for env level flags, so
        // we shouldn't run into a case where this is hit for those
        case FeatureFlagActionType.REMOVE:
            newFlags = state.featureFlags.filter(
                (f) => f !== payload.trim().toLowerCase(),
            );
            storeFeatureFlags(newFlags);
            return { featureFlags: newFlags };
        default:
            return state;
    }
};

export const FeatureFlagProvider = ({
    children,
}: React.PropsWithChildren<{}>) => {
    // reducer manages per user feature flags only
    const [{ featureFlags }, dispatch] = useReducer(featureFlagReducer, {
        featureFlags: getSavedFeatureFlags(),
    });

    // makes sure default values from environment don't get stored in local storage.
    // if the env turns on the flag, we don't want a user accidentally accessing it later on
    const allFeatureFlags: string[] = useMemo(
        () => uniq(featureFlags.concat(DEFAULT_FEATURE_FLAGS)),
        [featureFlags],
    );

    const checkFlag = useCallback(
        (flag: string): boolean => !!allFeatureFlags.find((el) => el === flag),
        [allFeatureFlags],
    );

    return (
        <FeatureFlagContext.Provider
            value={{
                dispatch,
                checkFlag,
                featureFlags: allFeatureFlags,
            }}
        >
            {children}
        </FeatureFlagContext.Provider>
    );
};

// an extra level of indirection here to allow for generic typing of the returned fetch function
export const useFeatureFlags = (): FeatureFlagContextValues =>
    useContext(FeatureFlagContext);
