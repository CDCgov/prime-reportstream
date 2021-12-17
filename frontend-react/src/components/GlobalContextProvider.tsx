import { createContext, useContext, useState } from "react";

/* INFO
   Please use these rather than hard-coded strings when
   referencing localStorage/sessionStorage to set, get, or remove items
   associated with global context
   These keys are
   */
enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
}

/* INFO
   Updating this default context model will allow us to expand on our
   global context offerings. We could store all kinds of preferences
   in here!
*/
export const GlobalContext = createContext({
    state: {
        organization: getStoredOrg() || "",
    },
    updateOrganization: (newOrganization: string): void => {
        /* Default placeholder function model */
    },
});

export function useGlobalContext() {
    return useContext(GlobalContext);
}

export function clearGlobalContext(): void {
    for (let key in sessionStorage) {
        if (key.includes(GLOBAL_STORAGE_KEYS.GLOBAL_BASE)) {
            sessionStorage.removeItem(key);
        }
    }
}

export function getStoredOrg(): string | undefined {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined;
}

function GlobalContextProvider({
    children,
}: {
    children: JSX.Element[];
}): JSX.Element {
    const [organization, setOrganization] = useState(getStoredOrg() || "");

    const updateOrganization = (newOrganization: string): void => {
        sessionStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, newOrganization);
        setOrganization(newOrganization);
        setContext({
            state: {
                organization: newOrganization,
            },
            updateOrganization: updateOrganization,
        });
    };

    /* INFO
       This is where we would add more functions like updateOrganiztion()
       if we wanted to have more update functions for future global context
       values
    */

    const [context, setContext] = useState({
        state: {
            organization: organization,
        },
        updateOrganization: updateOrganization,
    });

    return (
        <GlobalContext.Provider value={context}>
            {children}
        </GlobalContext.Provider>
    );
}

export default GlobalContextProvider;
