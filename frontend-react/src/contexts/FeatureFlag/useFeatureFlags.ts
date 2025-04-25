import { useContext } from "react";
import { FeatureFlagContext, FeatureFlagContextValues } from "./FeatureFlagProvider";

// an extra level of indirection here to allow for generic typing of the returned fetch function
const useFeatureFlags = (): FeatureFlagContextValues => useContext(FeatureFlagContext);

export default useFeatureFlags;
