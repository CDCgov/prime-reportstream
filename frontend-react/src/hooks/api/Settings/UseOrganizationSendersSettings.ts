import { useSessionContext } from "../../../contexts/SessionContext";
import { settingsEndpoints } from "../../../config/api/settings";
import { useRSQuery } from "../UseRSQuery";

export const useOrganizationSendersSettings = (organizationName: string) => {
    const { activeMembership } = useSessionContext();
    const orgName = organizationName ?? activeMembership?.parsedName;

    return useRSQuery(
        settingsEndpoints.senders,
        { segments: { orgName } },
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
        }
    );
};
