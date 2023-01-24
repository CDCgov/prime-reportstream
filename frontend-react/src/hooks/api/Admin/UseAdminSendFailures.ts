import { adminEndpoints } from "../../../config/api/admin";
import { useRSQuery } from "../UseRSQuery";

export const useAdminSendFailures = (days: number) => {
    return useRSQuery(adminEndpoints.sendFailures, {
        params: {
            days_to_show: days,
        },
    });
};
