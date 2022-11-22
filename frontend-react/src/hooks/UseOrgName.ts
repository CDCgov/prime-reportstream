<<<<<<< HEAD
import { useOrganizationResource } from "./UseOrganizationResource";

function useOrgName(): string {
    const { organization } = useOrganizationResource();

    return organization?.description || "";
}

export { useOrgName };
=======
import { useOrganizationResource } from "./UseOrganizationResource";

/**
 * @deprecated Please use useOrganizationSettings instead
 */
function useOrgName(): string {
    const { organization } = useOrganizationResource();

    return organization?.description || "";
}

export { useOrgName };
>>>>>>> 345f618243569d93f8f270a2d56c06f1c7b06d66
