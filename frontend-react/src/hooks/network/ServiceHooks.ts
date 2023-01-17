import {
    Dispatch,
    SetStateAction,
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";
import { AccessToken } from "@okta/okta-auth-js";
import pick from "lodash.pick";

import { auxExports } from "../UseCreateFetch";
import {
    MembershipState,
    MemberType,
    ServiceSettings,
} from "../UseOktaMemberships";
import { RSService, servicesEndpoints } from "../../config/endpoints/settings";
import { RSNetworkError } from "../../utils/RSNetworkError";

type ServiceState = ServiceSettings & {
    setActiveService: Dispatch<SetStateAction<string>>;
};

/** Prunes all settings data not relevant to our application session */
export const cleanServicesArray = (arr: RSService[]) => {
    return arr.map((obj) =>
        pick(obj, "name", "organizationName", "topic", "customerStatus")
    );
};

/** Fetches membership services (senders, receivers) from the ReportStream API. Updates
 * whenever the given {@link MembershipState} or AccessToken (Okta) are updated.
 * @remarks ONLY FOR USE WITHIN {@link SessionContext} */
export const useMemberServices = (
    state: MembershipState,
    token: AccessToken | undefined
): ServiceState => {
    const [senders, setSenders] = useState<RSService[] | undefined>();
    const [receivers, setReceivers] = useState<RSService[] | undefined>();
    const [activeService, setActiveService] = useState<string>("default");
    /* Because this is used at the SessionContext > useOktaMemberships level, it does not have access
     * to the AuthorizedFetchProvider nested within SessionContext. For this, we must generate an auth fetch
     * manually using our custom tooling. */
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
    /* Logic gate for whether we should or should not fetch */
    const hasAllNecessaryVariablesForFetch = useMemo(
        () =>
            state?.initialized &&
            state?.activeMembership?.memberType &&
            state?.activeMembership?.parsedName &&
            state?.activeMembership?.parsedName !== "PrimeAdmins" &&
            token,
        [
            state?.activeMembership?.memberType,
            state?.activeMembership?.parsedName,
            state?.initialized,
            token,
        ]
    );
    /* Using async auth fetches to retrieve arrays of sender/receiver services from API
     * and setting local state */
    useEffect(() => {
        const fetcher = authFetchServicesGenerator();
        const fetcherOptions = {
            segments: {
                orgName: state?.activeMembership?.parsedName!!,
            },
        };
        const fetchData = async () => {
            const { senders, receivers } = servicesEndpoints;
            const [receiverServiceResults, senderServiceResults] =
                await Promise.all([
                    fetcher<RSService[]>(receivers, fetcherOptions),
                    fetcher<RSService[]>(senders, fetcherOptions),
                ]);
            setSenders(cleanServicesArray(senderServiceResults));
            setReceivers(cleanServicesArray(receiverServiceResults));
        };
        if (hasAllNecessaryVariablesForFetch) {
            fetchData().catch((e: RSNetworkError) => console.error(e));
        }
    }, [
        hasAllNecessaryVariablesForFetch,
        authFetchServicesGenerator,
        state?.activeMembership?.memberType,
        state?.activeMembership?.parsedName,
    ]);

    return {
        activeService,
        senders,
        receivers,
        setActiveService,
    };
};
