import { Dispatch, SetStateAction, useState, useEffect } from "react";

import { RSReceiver } from "../config/endpoints/settings";
import { CustomerStatusType } from "../utils/DataDashboardUtils";

import { useOrganizationReceivers } from "./UseOrganizationReceivers";

interface ReceiverFeeds {
    loadingServices: boolean;
    services: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
    isDisabled: boolean;
}

export function sortAndFilterInactiveServices(
    services: RSReceiver[],
): RSReceiver[] {
    const filteredServices = services.filter(
        (service) => service.customerStatus !== CustomerStatusType.INACTIVE,
    );
    return filteredServices?.sort((a, b) => a.name.localeCompare(b.name)) || [];
}
/** Fetches a list of receiver services for your active organization, and provides a controller to switch
 * between them */
export const useOrganizationReceiversFeed = (): ReceiverFeeds => {
    const {
        data: receivers,
        isLoading,
        fetchStatus,
    } = useOrganizationReceivers();
    const [active, setActive] = useState<RSReceiver | undefined>();
    const [sortedAndFilteredServices, setSortedAndFilteredServices] = useState<
        RSReceiver[] | []
    >();
    const [receiversFound, setReceiversFound] = useState<boolean | undefined>();

    useEffect(() => {
        if (receivers?.length) {
            const sortedAndFilteredReceivers =
                sortAndFilterInactiveServices(receivers);
            setActive(
                sortedAndFilteredReceivers[0], // Defaults to first in array
            );
            setSortedAndFilteredServices(sortedAndFilteredReceivers);
            setReceiversFound(true);
        } else {
            setReceiversFound(false);
        }
    }, [receivers]);

    return {
        loadingServices:
            (isLoading && fetchStatus !== "idle") ||
            receiversFound === undefined,
        services: sortedAndFilteredServices || [],
        activeService: active,
        setActiveService: setActive,
        isDisabled: isLoading && fetchStatus === "idle",
    };
};
