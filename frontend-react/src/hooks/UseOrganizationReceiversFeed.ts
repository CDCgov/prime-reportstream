import { useState, useEffect } from "react";

import type { RSReceiver } from "../config/endpoints/settings";
import { CustomerStatusType } from "../utils/DataDashboardUtils";

import { useOrganizationReceivers } from "./UseOrganizationReceivers";

export type ReceiverFeeds = ReturnType<typeof useOrganizationReceiversFeed>;

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
export const useOrganizationReceiversFeed = (orgName?: string) => {
    const {
        data: receivers,
        isDisabled,
        ...query
    } = useOrganizationReceivers(orgName);
    const [active, setActive] = useState<RSReceiver | undefined>();
    const [data, setData] = useState<RSReceiver[]>();

    useEffect(() => {
        if (receivers?.length) {
            const sortedAndFilteredReceivers =
                sortAndFilterInactiveServices(receivers);
            setData(sortedAndFilteredReceivers);
            setActive(
                (v) => v ?? sortedAndFilteredReceivers[0], // Defaults to first in array
            );
        }
    }, [receivers]);

    return {
        ...query,
        data,
        activeService: active,
        setActiveService: setActive,
        isDisabled,
    };
};
