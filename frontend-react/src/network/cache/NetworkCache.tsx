import React, { Context, createContext, useContext, useReducer, useState } from "react";

import { Endpoint } from "../api/Api";
import { ResponseType } from "../hooks/useNetwork";

/* 
    Cache types and interfaces

    CacheItem<T>: A single item in the cache array that will compare
    incoming endpoint criteria to access the cached response IF the
    timeout has not expired.

    Cache: An array of CacheItems of any type.
*/
export type UpdateCacheParams = {
    endpoint: Endpoint;
    response: ResponseType<any>;
};

export interface CacheItem<T> {
    endpoint?: Endpoint;
    response?: ResponseType<T>;
    timeout: number;
}

interface Cache {
    store: Array<CacheItem<any>>;
    updateCache: ({ endpoint, response }: UpdateCacheParams) => void
}

/* Initialize empty cache and create Context */
const CacheContext: Context<Cache> = createContext({
    store: [] as Array<CacheItem<any>>,
    updateCache: ({ endpoint, response }: UpdateCacheParams) => {
        console.log('sample function')
    }
});

/* Helper function to create CacheItem */
function generateCacheItem({ endpoint, response }: UpdateCacheParams): CacheItem<any> {
    return {
        endpoint: endpoint,
        response: response,
        timeout: 600000,
    };
}

/* Cache provider */
function NetworkCache({ children }: { children: JSX.Element | JSX.Element[] }) {
    /* Elements of Cache interface as state */
    const [cacheStore, setCacheStore] = useState<Array<CacheItem<any>>>([]);

    /* Update the cache store using this function from components using 
    this provider */
    const updateCacheStore = ({ endpoint, response }: UpdateCacheParams) => {
        const cacheItem = generateCacheItem({ endpoint, response });
        setCacheStore([...cacheStore, cacheItem]);
    };

    /* I KNOW it breaks the best practice of maintaining state at the top of
    a component, but to circumvent "definition before declaration" errors, this
    has to be our last state item. */
    const [cache] = useState<Cache>({
        store: cacheStore,
        updateCache: updateCacheStore,
    });

    /* Wraps any children with the CacheProvider */
    return (
        <CacheContext.Provider value={cache}>{children}</CacheContext.Provider>
    );
}

export function useCacheContext() {
    return useContext(CacheContext);
}

export default NetworkCache;
