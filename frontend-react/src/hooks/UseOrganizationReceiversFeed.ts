import { Dispatch, SetStateAction, useState, useEffect } from "react";

import { RSReceiver } from "../config/endpoints/settings";

import { useOrganizationReceivers } from "./UseOrganizationReceivers";

interface ReceiverFeeds {
    loadingServices: boolean;
    services: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}
/** Fetches a list of receivers for your active organization, and provides a controller to switch
 * between them */
export const useOrganizationReceiversFeed = (): ReceiverFeeds => {
    const { data: receivers, isLoading } = useOrganizationReceivers();
    const [active, setActive] = useState<RSReceiver | undefined>();

    useEffect(() => {
        if (receivers?.length) {
            setActive(
                // Checks for an active receiver first
                receivers.find((val) => val.customerStatus === "active") ||
                    // Defaults to first in array
                    receivers[0]
            );
        }
    }, [receivers]);

    return {
        loadingServices: isLoading,
        services: receivers || [],
        activeService: active,
        setActiveService: setActive,
    };
};
