import * as StorageTools from "../SessionStorageTools";

export const mockStoreFeatureFlags = jest.spyOn(
    StorageTools,
    "storeFeatureFlags",
);

export const mockGetSavedFeatureFlags = jest.spyOn(
    StorageTools,
    "getSavedFeatureFlags",
);
