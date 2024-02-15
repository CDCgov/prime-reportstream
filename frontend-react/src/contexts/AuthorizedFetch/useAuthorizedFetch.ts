import { useContext } from "react";
import {
    AuthorizedFetchContext,
    IAuthorizedFetchContext,
} from "./AuthorizedFetchProvider";

// an extra level of indirection here to allow for generic typing of the returned fetch function
function useAuthorizedFetch<
    TQueryFnData = unknown,
>(): IAuthorizedFetchContext<TQueryFnData> {
    return useContext<IAuthorizedFetchContext<TQueryFnData>>(
        AuthorizedFetchContext,
    );
}

export default useAuthorizedFetch;
