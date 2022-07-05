import { useOrganizationResource } from "./UseOrganizationResouce";

function useOrgName(): string {
    const { organization } = useOrganizationResource();

    return organization?.description || "";
}

export { useOrgName };
