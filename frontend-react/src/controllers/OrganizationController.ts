import { useOktaAuth } from "@okta/okta-react";
import { useResource } from "rest-hooks";
import OrganizationResource from "../resources/OrganizationResource";
import { groupToOrg } from "../webreceiver-utils";

export function useOrgName(): string {
    const {authState} = useOktaAuth()
    const org = useResource(OrganizationResource.detail(), {
        name: groupToOrg(
            authState!.accessToken?.claims.organization.find((o: string) => !o.toLowerCase().includes('sender'))
        )
    });
    return org?.description || ""
}
