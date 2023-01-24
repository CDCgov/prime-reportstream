import { settingsEndpoints } from "../../../config/api/settings";
import { useRSQuery } from "../UseRSQuery";

export const useOrganizationsSettings = () => {
    return useRSQuery(settingsEndpoints.organization);
};
