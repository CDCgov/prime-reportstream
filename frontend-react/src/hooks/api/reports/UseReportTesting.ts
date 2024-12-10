import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";
import { reportsEndpoints, RSReceiver } from "../../../config/endpoints/settings";
import useSessionContext from "../../../contexts/Session/useSessionContext";
import { Organizations } from "../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { testing } = reportsEndpoints;

const useReportTesting = () => {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const parsedName = activeMembership?.parsedName;
    const isAdmin = Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;

    const memoizedDataFetch = useCallback(() => {
        if (isAdmin) {
            return authorizedFetch<RSReceiver[]>({}, testing);
        }
        return null;
    }, [isAdmin, authorizedFetch]);
    const useSuspenseQueryResult = useSuspenseQuery({
        queryKey: [testing.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });

    const { data } = useSuspenseQueryResult;

    return {
        ...useSuspenseQueryResult,
        testMessages: data,
        isDisabled: !isAdmin,
    };
};

export default useReportTesting;
