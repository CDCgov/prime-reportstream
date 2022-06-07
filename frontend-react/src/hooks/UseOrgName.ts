import { useResource } from "rest-hooks";

import OrganizationResource from "../resources/OrganizationResource";
import { getStoredOrg } from "../contexts/SessionStorageTools";
import { parseOrgName } from "../utils/OrganizationUtils";

function useOrgName(): string {
    const org = useResource(OrganizationResource.detail(), {
        orgname: parseOrgName(getStoredOrg()),
    });

    return org?.description || "";
}

export { useOrgName };
