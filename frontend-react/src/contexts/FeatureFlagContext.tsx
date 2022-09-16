import React, { createContext, useCallback, useContext, useReducer } from "react";
import uniq from 'lodash.uniq';

import config from "../config";
import { getSavedFeatureFlags } from "../utils/SessionStorageTools";

export enum FeatureFlagActionType {
  ADD = "ADD",
  REMOVE = "REMOVE",
}

export interface FeatureFlagAction {
  type: FeatureFlagActionType;
  // Only need to pass name of an org to swap to
  payload?: string;
}

type StringCheck = {
  (flag: string): boolean
}

interface FeatureFlagContextValues {
  checkFlag: StringCheck,
  dispatch: React.Dispatch<FeatureFlagAction>,
}

type FeatureFlagState {
  featureFlags: string[]
}

const { DEFAULT_FEATURE_FLAGS } = config;

export const FeatureFlagContext = createContext<FeatureFlagContextValues>({
    checkFlag: (flag: string) => !!DEFAULT_FEATURE_FLAGS.find(flag),
    dispatch: () => {},
});

const featureFlagReducer = (
  state: FeatureFlagState,
  action: FeatureFlagAction) => {
    const { type, payload } = action;
    switch (type) {
        case FeatureFlagActionType.ADD:
            return newState;
        case FeatureFlagActionType.REMOVE:
            return newState;
        default:
            return state;
    }
}


const getInitialFeatureFlags = (): string[] => {
  const savedFlags = getSavedFeatureFlags();
  return uniq(savedFlags.concat(DEFAULT_FEATURE_FLAGS));
};


export const FeatureFlagProvider = ({
    children,
}: React.PropsWithChildren<{}>) => {
    const [{ featureFlags }, dispatch] = useReducer(featureFlagReducer, { featureFlags: getInitialFeatureFlags() });
    
    const checkFlag = useCallback((flag: string): boolean => !!featureFlags.find(flag), [featureFlags]);

    return (
        <FeatureFlagContext.Provider
            value={{
                dispatch,
                checkFlag,
            }}
        >
            {children}
        </FeatureFlagContext.Provider>
    );
};

// an extra level of indirection here to allow for generic typing of the returned fetch function
export const useFeatureFlags = (): FeatureFlagContextValues => {
    return useContext(FeatureFlagContext);
};
