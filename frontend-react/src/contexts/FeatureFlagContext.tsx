import { useCache } from "@rest-hooks/core/lib/react-integration/newhooks";
import React, { createContext, useCallback, useContext, useReducer } from "react";

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
  // featureFlags: string[],
  checkFlag: StringCheck,
  dispatch: React.Dispatch<FeatureFlagAction>,
}

type FeatureFlagState {
  featureFlags: string[]
}

export const FeatureFlagContext = createContext<FeatureFlagContextValues>({
    // featureFlags: [],
    checkFlag: () => false,
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


export const FeatureFlagProvider = ({
    children,
}: React.PropsWithChildren<{}>) => {
    const [{ featureFlags }, dispatch] = useReducer(featureFlagReducer, { featureFlags: [] });
    
    const checkFlag = useCallback((flag: string): boolean => !!featureFlags.find(flag), [featureFlags]);

    return (
        <FeatureFlagContext.Provider
            value={{
                // featureFlags,
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
