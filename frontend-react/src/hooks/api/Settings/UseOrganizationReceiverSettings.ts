import { settingsEndpoints } from "../../../config/api/settings";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useRSQuery } from "../UseRSQuery";

export const useOrganizationReceiverSettings = (
    receiverName: string,
    organizationName?: string
) => {
    const { activeMembership } = useSessionContext();
    const orgName = organizationName ?? activeMembership?.parsedName;

    return useRSQuery(
        settingsEndpoints.receiver,
        {
            segments: {
                orgName,
                receiverId: receiverName,
            },
        },
        {
            enabled: !!activeMembership?.parsedName,
        }
    );
};
