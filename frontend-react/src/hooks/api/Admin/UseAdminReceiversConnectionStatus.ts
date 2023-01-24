import { adminEndpoints } from "../../../config/api/admin";
import { useRSQuery } from "../UseRSQuery";

export const useAdminReceiversConnectionStatus = (
    startDate: Date,
    endDate?: Date
) => {
    return useRSQuery(adminEndpoints.listReceiversConnectionStatus, {
        params: {
            // eslint-disable-next-line camelcase
            start_date: startDate.toISOString(),
            // eslint-disable-next-line camelcase
            end_date: endDate?.toISOString(),
        },
    });
};
