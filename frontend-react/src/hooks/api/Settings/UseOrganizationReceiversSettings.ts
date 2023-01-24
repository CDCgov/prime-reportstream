import { settingsEndpoints } from "../../../config/api/settings";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useRSQuery } from "../UseRSQuery";

export const useOrganizationReceiversSettings = (organizationName?: string) => {
    const { activeMembership } = useSessionContext();
    const orgName = organizationName ?? activeMembership?.parsedName;

    return useRSQuery(
        settingsEndpoints.receivers,
        { segments: { orgName } },
        { enabled: !!activeMembership?.parsedName }
    );
};
