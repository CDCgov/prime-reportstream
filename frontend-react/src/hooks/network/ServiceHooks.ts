import { useCallback, useEffect, useMemo, useState } from "react";
import { AccessToken } from "@okta/okta-auth-js";

import { auxExports } from "../UseCreateFetch";
import {
    MembershipState,
    MemberType,
    ServiceSettings,
} from "../UseOktaMemberships";
import { RSService, servicesEndpoints } from "../../config/endpoints/settings";
import { RSNetworkError } from "../../utils/RSNetworkError";

export const useMemberServices = (
    state: MembershipState,
    token: AccessToken | undefined
): ServiceSettings => {
    const [services, setServices] = useState<ServiceSettings>({
        activeService: undefined,
        senders: [],
        receivers: [],
    });
    // Callback for generating the fetcher, moved outside useEffect to reduce effect dependencies
    const authFetchServicesGenerator = useCallback(() => {
        return auxExports.createTypeWrapperForAuthorizedFetch(token!!, {
            parsedName: state?.activeMembership?.parsedName || "",
            memberType:
                state?.activeMembership?.memberType || MemberType.NON_STAND,
        });
    }, [
        state?.activeMembership?.memberType,
        state?.activeMembership?.parsedName,
        token,
    ]);

    const hasAllNecessaryVariablesForFetch = useMemo(
        () =>
            state?.initialized &&
            state?.activeMembership?.memberType &&
            state?.activeMembership?.parsedName &&
            token,
        [
            state?.activeMembership?.memberType,
            state?.activeMembership?.parsedName,
            state?.initialized,
            token,
        ]
    );

    // Performs the fetch and membership state update
    useEffect(() => {
        const fetcher = authFetchServicesGenerator();
        const fetcherOptions = {
            segments: {
                orgName: state?.activeMembership?.parsedName!!,
            },
        };
        const fetchData = async () => {
            const { senders, receivers } = servicesEndpoints;
            const senderServiceResults: RSService[] = await fetcher<
                RSService[]
            >(senders, fetcherOptions);
            const receiverServiceResults: RSService[] = await fetcher<
                RSService[]
            >(receivers, fetcherOptions);
            setServices({
                activeService: undefined,
                senders: senderServiceResults,
                receivers: receiverServiceResults,
            });
        };
        if (hasAllNecessaryVariablesForFetch) {
            if (state?.activeMembership?.parsedName !== "PrimeAdmins") {
                fetchData().catch((e: RSNetworkError) => console.error(e));
            }
        }
    }, [
        hasAllNecessaryVariablesForFetch,
        authFetchServicesGenerator,
        state?.activeMembership?.memberType,
        state?.activeMembership?.parsedName,
    ]);

    return services;
};
