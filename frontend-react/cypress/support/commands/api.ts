import { GLOBAL_STORAGE_KEYS } from "../../../src/utils/SessionStorageTools";

declare global {
    namespace Cypress {
        interface Chainable {
            fetchReceivers(organizationName: string): Chainable<any>;

            fetchDeliveries(
                organizationName: string,
                receiverName: string
            ): Chainable<any>;

            fetchSubmissions(
                organizationName: string,
                sender: string
            ): Chainable<any>;

            fetchLookupTables(): Chainable<any>;

            fetchLookupTableContent(tableName: string): Chainable<any>;
        }
    }
}

// NOTE: all of these commands expect cy.login() to have been called beforehand
const accessToken = window.localStorage.getItem(
    GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN
);

const requestOptions = {
    headers: {
        authorization: `Bearer ${accessToken}`,
    },
    timeout: 3 * 60 * 1000,
};

export function fetchReceivers(organizationName: string) {
    // TODO: consolidate with OrgReceiverSettingResource
    return cy.request({
        ...requestOptions,
        url: `/api/settings/organizations/${organizationName}/receivers`,
    });
}

export function fetchDeliveries(
    organizationName: string,
    receiverName: string
) {
    // TODO: consolidate with SubmissionsResource
    // TODO: allow customizable query parameters
    return cy.request({
        ...requestOptions,
        url: `/api/waters/org/${organizationName}.${receiverName}/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61`,
    });
}

export function fetchSubmissions(organizationName: string) {
    // TODO: consolidate with SubmissionsResource
    // TODO: allow customizable query parameters
    return cy.request({
        ...requestOptions,
        url: `/api/waters/org/${organizationName}/submissions?pageSize=61&cursor=3000-01-01T00%3A00%3A00.000Z&since=2000-01-01T00%3A00%3A00.000Z&until=3000-01-01T00%3A00%3A00.000Z&sortcol=undefined&sortdir=DESC&showfailed=false`,
    });
}

export function fetchLookupTables() {
    return cy.request({
        ...requestOptions,
        url: "/api/lookuptables/list",
    });
}

export function fetchLookupTableContent(tableName: string) {
    return cy.request({
        ...requestOptions,
        url: `/api/lookuptables/${tableName}/content`,
    });
}

Cypress.Commands.add("fetchReceivers", fetchReceivers);
Cypress.Commands.add("fetchDeliveries", fetchDeliveries);
Cypress.Commands.add("fetchSubmissions", fetchSubmissions);
Cypress.Commands.add("fetchLookupTables", fetchLookupTables);
Cypress.Commands.add("fetchLookupTableContent", fetchLookupTableContent);
