import * as StorageTools from "../SessionStorageTools";

/* I could not get jest's auto-mock feature to work, so instead I figured we could export
 * spies from these __mocks__ directories to help cut down the need to write all these
 * spies every time. */
export const mockTokenFromStorage = jest.spyOn(
    StorageTools,
    "getStoredOktaToken",
);

export const mockStoreFeatureFlags = jest.spyOn(
    StorageTools,
    "storeFeatureFlags",
);

export const mockGetSavedFeatureFlags = jest.spyOn(
    StorageTools,
    "getSavedFeatureFlags",
);
