import { Endpoint } from "../api/Api";
import {
    CacheItem,
    UpdateCacheParams,
    useCacheContext,
} from "../cache/NetworkCache";

export function useCache(endpoint: Endpoint): CacheItem<any> | undefined {
    /* Read in our store and update function from NetworkCache.tsx */
    const { store } = useCacheContext();
    debugger;
    /* Traverse store to find matching endpoint call w/ response */
    store.map((item: CacheItem<any>) => {
        debugger;
        if (item?.endpoint == endpoint && item?.response) {
            /* Return CacheItem */
            return item;
        }
    });
    /* Whoops! No CacheItem for you! */
    return undefined;
}
