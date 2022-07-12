import { useMemo } from "react";

import { useSessionContext } from "../../../contexts/SessionContext";
import {
    BasicAPIResponse,
    createRequestConfig,
} from "../../../network/api/NewApi";
import ReportsApi, {
    RSReportInterface,
} from "../../../network/api/History/Reports";
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

export { useReportsList };
