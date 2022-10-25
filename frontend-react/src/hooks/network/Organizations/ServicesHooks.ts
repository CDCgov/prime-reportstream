import { useCallback, useMemo } from "react";

import { MemberType } from "../../UseOktaMemberships";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { useAdminSafeOrgName } from "../UseMemoizedConfig";
import { servicesEndpoints } from "../../../config/endpoints/services";

/** Response is much larger than this but not all of it is used for front-end yet */
export interface RSService {
    name: string;
    organizationName: string;
    topic: string;
    customerStatus: string;
}

interface ServicesResponse {
    servicesArray: RSService[] | undefined;
}

const { senders, receivers } = servicesEndpoints;

export const useMembershipServices = (
    memberType: MemberType | undefined,
    orgName: string | undefined
): ServicesResponse => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSService[]>();
    const adminSafeOrgName = useAdminSafeOrgName(orgName); // "PrimeAdmins" -> "ignore"
    const memberTypeServiceEndpoint = useMemo(
        () => (memberType === MemberType.SENDER ? senders : receivers),
        [memberType]
    );
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(memberTypeServiceEndpoint, {
                segments: {
                    orgName: adminSafeOrgName!!,
                },
            }),
        [adminSafeOrgName, authorizedFetch, memberTypeServiceEndpoint]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when swapping services
        [memberTypeServiceEndpoint.queryKey, { adminSafeOrgName }],
        memoizedDataFetch,
        { enabled: !!orgName && !!memberType }
    );
    return { servicesArray: data };
};
