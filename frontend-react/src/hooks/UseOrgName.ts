import { useOrganizationResource } from "./UseOrganizationResource";

function useOrgName(): string {
    const { organization } = useOrganizationResource();

    return organization?.description || "";
}

export { useOrgName };
