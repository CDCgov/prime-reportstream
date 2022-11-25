import { useOrganizationResource } from "./UseOrganizationResource";

/**
 * @deprecated Please use useOrganizationSettings instead
 */
function useOrgName(): string {
    const { organization } = useOrganizationResource();

    return organization?.description || "";
}

export { useOrgName };
