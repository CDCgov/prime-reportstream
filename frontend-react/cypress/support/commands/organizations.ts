import { GLOBAL_STORAGE_KEYS } from "../../../src/utils/SessionStorageTools";

declare global {
    namespace Cypress {
        interface Chainable {
            setOrganization(organizationName: string): Chainable<any>;
        }
    }
}

const DEFAULT_ACTIVE_MEMBERSHIP = {
    memberType: "prime-admin",
    service: "default",
};

function setOrganization(parsedName: string) {
    const activeMembership = {
        ...DEFAULT_ACTIVE_MEMBERSHIP,
        parsedName,
    };

    window.localStorage.setItem(
        GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE,
        JSON.stringify({
            activeMembership,
            initialized: true,
        })
    );

    window.localStorage.setItem(
        GLOBAL_STORAGE_KEYS.ORGANIZATION_OVERRIDE,
        JSON.stringify(activeMembership)
    );
}

Cypress.Commands.add("setOrganization", setOrganization);
