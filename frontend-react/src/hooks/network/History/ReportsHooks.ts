import { useMemo } from "react";

import { useSessionContext } from "../../../contexts/SessionContext";
import {
    BasicAPIResponse,
    createRequestConfig,
} from "../../../network/api/NewApi";
import ReportsApi, {
    ReportDetailParams,
    RSReportInterface,
} from "../../../network/api/History/Reports";
import useRequestConfig from "../UseRequestConfig";

const useReportsList = () => {
    const { memberships, activeMembership, oktaToken } = useSessionContext();
    const adminSafeOrgName = useMemo(
        () =>
            activeMembership?.parsedName === "PrimeAdmins"
                ? "ignore"
                : activeMembership?.parsedName,
        [activeMembership?.parsedName]
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
    const { data, error, loading, trigger } = useRequestConfig(
        config
    ) as BasicAPIResponse<RSReportInterface[]>;

    return {
        data,
        error,
        loading,
        trigger,
    };
};

const useReportsDetail = (reportId: string) => {
    const { activeMembership, oktaToken } = useSessionContext();
    const adminSafeOrgName = useMemo(
        () =>
            activeMembership?.parsedName === "PrimeAdmins"
                ? "ignore"
                : activeMembership?.parsedName,
        [activeMembership?.parsedName]
    );
    const config = useMemo(
        () =>
            createRequestConfig<ReportDetailParams>(
                ReportsApi,
                "detail",
                "GET",
                oktaToken?.accessToken,
                adminSafeOrgName,
                { id: reportId }
            ),
        [oktaToken?.accessToken, adminSafeOrgName, reportId]
    );
    const { data, error, loading, trigger } = useRequestConfig(
        config
    ) as BasicAPIResponse<RSReportInterface>;

    return {
        data,
        error,
        loading,
        trigger,
    };
};

export { useReportsList, useReportsDetail };
