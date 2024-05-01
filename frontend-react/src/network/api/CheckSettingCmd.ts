import { useMutation } from "@tanstack/react-query";

import { HTTPMethods, RSEndpoint } from "../../config/endpoints";
import useAuthorizedFetch from "../../contexts/AuthorizedFetch/useAuthorizedFetch";
import { RSNetworkError } from "../../utils/RSNetworkError";

/**
 * See prime-router/docs/api/check.yml for documentation
 */
export interface CheckSettingResult {
    result: "success" | "fail" | ""; // "" is client-side uninitialized state
    message: string;
}

/** parameters used for the request. Also used by the react page to make passing data down easier **/
export interface CheckSettingParams {
    orgName: string;
    receiverName: string;
}

export const useCheckSettingsCmd = () => {
    const authorizedFetch = useAuthorizedFetch<CheckSettingResult>();
    const checkSettingsCmd = new RSEndpoint({
        path: "/checkreceiver/org/:orgName/receiver/:receiverName",
        method: HTTPMethods.POST,
        queryKey: "checkReceiverSettings",
    });

    const updateValueSet = (params: CheckSettingParams) => {
        return authorizedFetch(checkSettingsCmd, {
            segments: { ...params },
        });
    };
    return useMutation<CheckSettingResult, RSNetworkError, CheckSettingParams>({
        mutationFn: updateValueSet,
    });
};
