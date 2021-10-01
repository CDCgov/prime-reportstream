// import { useOktaAuth } from '@okta/okta-react';
import { createContext, useContext, useState } from 'react'

export type State = {
    organization: string,
}

export const GlobalContext = createContext({
    state: {
        organization: "",
    },
    updateOrganization: (newOrganization: string) => { console.log("Default") }
});
export function useGlobalContext() { return useContext(GlobalContext) }

function GlobalContextProvider({ children }) {

    // const { oktaAuth, authState } = useOktaAuth();
    const [organization, setOrganization] = useState("")

    const updateOrganization = (newOrganization: string): void => {
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
