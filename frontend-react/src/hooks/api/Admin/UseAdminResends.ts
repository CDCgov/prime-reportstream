import { adminEndpoints } from "../../../config/api/admin";
import { useRSQuery, UseRSQueryOptions } from "../UseRSQuery";

export function useAdminResends<
    T extends UseRSQueryOptions<(typeof adminEndpoints)["resend"]>
>(days: number, rsOptions?: T) {
    return useRSQuery(
        adminEndpoints.resend,
        {
            params: {
                days_to_show: days,
            },
        },
        rsOptions
    );
}
