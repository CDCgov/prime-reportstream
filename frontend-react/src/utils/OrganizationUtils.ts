import { useResource } from "rest-hooks";
import { GLOBAL_STORAGE_KEYS } from "../components/GlobalContextProvider";
import OrganizationResource from "../resources/OrganizationResource";
import { groupToOrg } from "../webreceiver-utils";

export function useOrgName(): string {
    const org = useResource(OrganizationResource.detail(), {
        name: groupToOrg(
            localStorage?.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined
        )
    });
    return org?.description || ""
}