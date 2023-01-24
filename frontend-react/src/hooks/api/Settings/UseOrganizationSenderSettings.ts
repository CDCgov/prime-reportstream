import { useSessionContext } from "../../../contexts/SessionContext";
import { settingsEndpoints } from "../../../config/api/settings";
import { useRSQuery } from "../UseRSQuery";

export const useOrganizationSenderSettings = (senderName?: string) => {
    const { activeMembership } = useSessionContext();
    const sender = senderName ?? activeMembership?.service!!;

    return useRSQuery(
        settingsEndpoints.sender,
        { segments: { orgName: activeMembership?.parsedName!!, sender } },
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
        }
    );
};
