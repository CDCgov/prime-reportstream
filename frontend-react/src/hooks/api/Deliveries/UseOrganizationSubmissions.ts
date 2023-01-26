import { deliveriesEndpoints } from "../../../config/api/deliveries";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useRSQuery, UseRSQueryOptions } from "../UseRSQuery";

export function useOrganizationSubmissions<
    T extends UseRSQueryOptions<
        (typeof deliveriesEndpoints)["orgAndServiceSubmissions"]
    >
>(name?: string, options?: T) {
    const { activeMembership } = useSessionContext();
    const orgName = name ?? activeMembership?.parsedName!!;

    return useRSQuery(
        deliveriesEndpoints.orgAndServiceSubmissions,
        {
            segments: {
                orgAndService: orgName,
            },
        },
        options
    );
}
