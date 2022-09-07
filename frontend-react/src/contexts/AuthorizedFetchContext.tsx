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

// an extra level of indirection here to allow for generic typing of the returned fetch function
export const useAuthorizedFetch = <T,>(): AuthorizedFetcher<T> => {
    const { authorizedFetchGenerator } = useContext(AuthorizedFetchContext);
    return authorizedFetchGenerator<T>();
};
