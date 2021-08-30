import {AuthState} from "@okta/okta-auth-js";
import {PERMISSIONS} from "./resources/PermissionsResource";


const groupToOrg = (group: String | undefined): string => {
    // in order to replace all instances of the underscore we needed to use a
    // global regex instead of a string. a string pattern only replaces the first
    // instance
    const re = /_/g;
    return group
        ? group.toUpperCase().startsWith("DH")
            ? group.slice(2).replace(re, "-")
            : group.replace(re, "-")
        : "";
};

const permissionCheck = (permission: String, authState: AuthState) => {
    if (permission === PERMISSIONS['receiver']) {
        return reportReceiver(authState);
    }
    return authState.accessToken?.claims.organization.find(o => o.includes(permission));
};

// A receiver is anyone with an organization that is not "DHSender", i.e.: "DHaz_phd"
const reportReceiver = (authState: AuthState) => {return authState.accessToken?.claims.organization.find(o => !o.includes(PERMISSIONS['sender']))};

export { groupToOrg, permissionCheck, reportReceiver };