import { settingsEndpoints } from "../../../config/api/settings";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useRSQuery } from "../UseRSQuery";

export const useOrganizationSettings = (name?: string) => {
    const { activeMembership } = useSessionContext();
    const orgName = name ?? activeMembership?.parsedName!!;

    return useRSQuery(
        settingsEndpoints.organization,
        { segments: { orgName } },
        { enabled: !!activeMembership?.parsedName }
    );
};
