import { adminEndpoints } from "../../../config/api/admin";
import { useRSQuery } from "../UseRSQuery";

export const useAdminResends = (days: number) => {
    return useRSQuery(adminEndpoints.resend, {
        params: {
            days_to_show: days,
        },
    });
};
