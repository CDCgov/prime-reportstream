import React, { createContext, useContext } from "react";
import { AccessToken } from "@okta/okta-auth-js";

import {
    useCreateFetch,
    AuthorizedFetchTypeWrapper,
    AuthorizedFetcher,
} from "../hooks/UseCreateFetch";
import { MembershipSettings } from "../hooks/UseOktaMemberships";

import { useSessionContext } from "./SessionContext";

interface IAuthorizedFetchContext {
    authorizedFetchGenerator: AuthorizedFetchTypeWrapper;
}
export const AuthorizedFetchContext = createContext<IAuthorizedFetchContext>({
    authorizedFetchGenerator: () => () =>
        Promise.reject("fetcher uninitialized"),
});

export const AuthorizedFetchProvider = ({
    children,
}: React.PropsWithChildren<{}>) => {
    const { oktaToken, activeMembership } = useSessionContext();
    const generator = useCreateFetch(
        oktaToken as AccessToken,
        activeMembership as MembershipSettings
    );

    return (
        <AuthorizedFetchContext.Provider
            value={{
                authorizedFetchGenerator: generator,
            }}
        >
            {children}
        </AuthorizedFetchContext.Provider>
    );
};

// we can refactor this when we introduce resources,
// but if this takes a resource that contains a list of configs
// we can likely avoid taking the path and method args at request time, and just read from the resource somehow
// and all that is passed in are things that would change at request time (path params, query params, custom headers, payload, etc.)
export const useAuthorizedFetch = <T,>(): AuthorizedFetcher<T> => {
    const { authorizedFetchGenerator } = useContext(AuthorizedFetchContext);
    return authorizedFetchGenerator<T>();
};
