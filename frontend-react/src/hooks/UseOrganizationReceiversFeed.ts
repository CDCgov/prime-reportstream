import { Dispatch, SetStateAction, useState, useEffect } from "react";

import { RSReceiver } from "../config/endpoints/settings";
import { CustomerStatus } from "../utils/TemporarySettingsAPITypes";

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
                receivers.find(
                    // Checks for an active receiver first
                    (val) => val.customerStatus === CustomerStatus.ACTIVE
                ) || receivers[0] // Defaults to first in array
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
