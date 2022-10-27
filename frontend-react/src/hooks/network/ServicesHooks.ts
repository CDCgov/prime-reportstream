import { useCallback, useEffect, useMemo, useState } from "react";
import { AccessToken } from "@okta/okta-auth-js";

import { auxExports } from "../UseCreateFetch";
import { RSService, servicesEndpoints } from "../../config/endpoints/services";
import { MembershipState, MemberType } from "../UseOktaMemberships";

export const useSessionServices = (
    state: MembershipState,
    token: AccessToken | undefined
) => {
    const [result, setResult] = useState<RSService[] | undefined>();
    const { activeMembership, initialized } = state;
    // To save any undefined check headaches in the useEffect
    const hasAllNecessaryVariablesForFetch = useMemo(
        () =>
            initialized &&
            !!activeMembership?.memberType &&
            !!activeMembership?.parsedName &&
            token,
        [
            activeMembership?.memberType,
            activeMembership?.parsedName,
            initialized,
            token,
        ]
    );
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
    // Performs the fetch and membership state update
    useEffect(() => {
        if (hasAllNecessaryVariablesForFetch) {
            const { senders, receivers } = servicesEndpoints;
            const fetcher = authFetchServicesGenerator();
            let fetchConfig;
            // Set the right endpoint (sender or receiver)
            if (activeMembership?.memberType === MemberType.SENDER) {
                fetchConfig = senders;
            }
            if (activeMembership?.memberType === MemberType.RECEIVER) {
                fetchConfig = receivers;
            }
            // Execute the call
            if (
                !!fetchConfig &&
                activeMembership?.parsedName !== "PrimeAdmins"
            ) {
                fetcher<RSService[]>(fetchConfig, {
                    segments: {
                        orgName: activeMembership?.parsedName!!,
                    },
                })
                    .then((res) => {
                        setResult(res);
                    })
                    .catch((e) => {
                        console.error(
                            `ERROR FETCHING SERVICES: ${activeMembership?.parsedName}:`,
                            e
                        );
                    });
            }
        }
    }, [
        hasAllNecessaryVariablesForFetch,
        authFetchServicesGenerator,
        activeMembership?.memberType,
        activeMembership?.parsedName,
    ]);

    return result;
};
