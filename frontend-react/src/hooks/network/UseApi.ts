import { Method } from "axios";
import { useMemo } from "react";

import { API, createRequestConfig } from "../../network/api/NewApi";
import { useSessionContext } from "../../contexts/SessionContext";

const useEndpoint = (api: API, endpointKey: string, method: Method) => {
    const { oktaToken, memberships } = useSessionContext();
    const requestConfig = useMemo(() => {
        return createRequestConfig(
            api,
            endpointKey,
            method,
            oktaToken?.accessToken,
            memberships.state.active?.parsedName
        );
    }, [
        api,
        endpointKey,
        memberships.state.active?.parsedName,
        method,
        oktaToken?.accessToken,
    ]);
};

export default useEndpoint;
