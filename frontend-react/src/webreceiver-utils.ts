import {AuthState} from "@okta/okta-auth-js";
import {PERMISSIONS} from "./resources/PermissionsResource";

const groupToOrg = ( group: String ) => { return group.slice(2).replace('_','-')}

const permissionCheck = ( permission: String, authState: AuthState ) => {
    if(permission === PERMISSIONS['receiver']) {
        return reportReceiver(authState);
    }
    return authState.accessToken?.claims.organization.find(o => o === permission);
};

const reportReceiver = (authState: AuthState) => {return authState.accessToken?.claims.organization.find(o => o !== PERMISSIONS['sender'])};

export { groupToOrg, permissionCheck, reportReceiver }