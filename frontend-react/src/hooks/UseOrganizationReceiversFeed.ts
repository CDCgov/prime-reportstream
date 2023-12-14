import { useState, useEffect } from "react";

import { CustomerStatus, type RsReceiver } from "../config/endpoints/settings";

import { useOrganizationReceivers } from "./UseOrganizationReceivers";

export type ReceiverFeeds = ReturnType<typeof useOrganizationReceiversFeed>;

export function sortAndFilterInactiveServices(
    services: RsReceiver[],
): RsReceiver[] {
    const filteredServices = services.filter(
        (service) => service.customerStatus !== CustomerStatus.INACTIVE,
    );
    return filteredServices?.sort((a, b) => a.name.localeCompare(b.name)) || [];
}
/** Fetches a list of receiver services for your active organization, and provides a controller to switch
 * between them */
export const useOrganizationReceiversFeed = () => {
    const {
        data: receivers,
        isDisabled,
        ...query
    } = useOrganizationReceivers();
    const [active, setActive] = useState<RsReceiver | undefined>();
    const [data, setData] = useState<RsReceiver[]>();

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
