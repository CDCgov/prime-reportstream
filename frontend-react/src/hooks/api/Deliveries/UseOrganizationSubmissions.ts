import { deliveriesEndpoints } from "../../../config/api/deliveries";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useRSQuery } from "../UseRSQuery";

export const useOrganizationSubmissions = (
    name: string,
    searchParams: string
) => {
    const { activeMembership } = useSessionContext();
    const orgName = name ?? activeMembership?.parsedName!!;

    return useRSQuery(deliveriesEndpoints.orgAndServiceSubmissions, {
        segments: {
            orgAndService: orgName,
        },
        params: searchParams,
    });
};
