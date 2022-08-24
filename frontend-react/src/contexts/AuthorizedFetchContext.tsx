import React, { createContext, useCallback, useContext } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import axios, { Method, AxiosRequestConfig } from "axios";
import { MembershipSettings } from "../hooks/UseOktaMemberships";

import { useSessionContext } from "./SessionContext";

// this should be contained in a config file
// TODO: move all config specific global variables to a config file
export const API_ROOT = `${process.env.REACT_APP_BACKEND_URL}/api`;

export interface AuthorizedFetchContext {}

interface AuthorizedFetchProviderProps {}

type AuthorizedFetcher = () => Promise<unknown>;

export const AuthorizedFetchContext = createContext<AuthorizedFetchContext>({
    authorizedFetch: () => Promise.reject("fetcher uninitialized"),
});

type AuthorizedFetchConfig = {
    path: string;
    verb: Method;
    options: Partial<AxiosRequestConfig>;
};

export const authorizedFetchFor = (
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
) => {};

export const AuthorizedFetchProvider = ({
    children,
}: React.PropsWithChildren<AuthorizedFetchProviderProps>) => {
    const { oktaToken, activeMembership } = useSessionContext();

    const authorizedFetch = useCallback(
        () =>
            authorizedFetchFor(
                oktaToken as Partial<AccessToken>,
                activeMembership
            ),
        [oktaToken, activeMembership]
    );
    return (
        <AuthorizedFetchContext.Provider
            value={{
                authorizedFetch,
            }}
        >
            {children}
        </AuthorizedFetchContext.Provider>
    );
};

export const useAuthorizedFetch = () => useContext(AuthorizedFetchContext);
