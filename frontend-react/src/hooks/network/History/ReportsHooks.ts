import { useMemo } from "react";

import { useSessionContext } from "../../../contexts/SessionContext";
import { createRequestConfig } from "../../../network/api/NewApi";
import ReportsApi, { TempRSReport } from "../../../network/api/History/Reports";
import useRequestConfig from "../UseRequestConfig";

const useReportsList = () => {
    const { memberships, oktaToken } = useSessionContext();
    const adminSafeOrgName = useMemo(
        () =>
            memberships.state.active?.parsedName === "PrimeAdmins"
                ? "ignore"
                : memberships.state.active?.parsedName,
        [memberships.state.active?.parsedName]
    );
    const config = useMemo(
        () =>
            createRequestConfig(
                ReportsApi,
                "list",
                "GET",
                oktaToken?.accessToken,
                adminSafeOrgName
            ),
        [oktaToken?.accessToken, adminSafeOrgName]
    );
    const {
        data: reports,
        error,
        loading,
        trigger,
    } = useRequestConfig(config) as {
        data: TempRSReport[];
        error: string;
        loading: boolean;
        trigger: () => void;
    };

    return {
        reports,
        error,
        loading,
        trigger,
    };
};

export default useReportsList;
