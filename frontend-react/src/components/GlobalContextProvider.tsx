// import { useOktaAuth } from '@okta/okta-react';
import { createContext, useContext, useState } from 'react'

export enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
}

export type State = {
    organization: string,
}

export const GlobalContext = createContext({
    state: {
        organization: localStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || "",
    },
    updateOrganization: (newOrganization: string) => { console.log("Default") }
});
export function useGlobalContext() { return useContext(GlobalContext) }

function GlobalContextProvider({ children }) {

    // const { oktaAuth, authState } = useOktaAuth();
    const [organization, setOrganization] = useState(localStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || "")

    const updateOrganization = (newOrganization: string): void => {
        localStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, newOrganization)
        setOrganization(newOrganization)
        setContext({
            state: {
                organization: newOrganization
            },
            updateOrganization: updateOrganization
        })
    }

    const [context, setContext] = useState({
        state: {
            organization: organization
        },
        updateOrganization: updateOrganization
    });

    return (
        <GlobalContext.Provider value={context}>
            {children}
        </GlobalContext.Provider>
    )
}

export default GlobalContextProvider
