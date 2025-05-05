import * as StorageTools from "../SessionStorageTools";

export const mockStoreFeatureFlags = vi.spyOn(StorageTools, "storeFeatureFlags");

export const mockGetSavedFeatureFlags = vi.spyOn(StorageTools, "getSavedFeatureFlags");
