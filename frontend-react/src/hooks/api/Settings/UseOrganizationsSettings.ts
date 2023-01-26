import { settingsEndpoints } from "../../../config/api/settings";
import { useRSQuery, UseRSQueryOptions } from "../UseRSQuery";

export function useOrganizationsSettings<
    T extends UseRSQueryOptions<(typeof settingsEndpoints)["organizations"]>
>(options: T) {
    return useRSQuery(settingsEndpoints.organizations, undefined, options);
}
