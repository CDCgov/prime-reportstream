import { useResource } from "rest-hooks";

import OrganizationResource from "../resources/OrganizationResource";
import { getStoredOrg } from "../contexts/SessionStorageTools";
import { groupToOrg } from "../utils/OrganizationUtils";

function useOrgName(): string {
    const org = useResource(OrganizationResource.detail(), {
        orgname: groupToOrg(getStoredOrg()),
    });

    return org?.description || "";
}

export { useOrgName };
