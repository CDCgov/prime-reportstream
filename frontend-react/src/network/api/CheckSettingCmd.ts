import { useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../../contexts/AuthorizedFetchContext";
import { RSNetworkError } from "../../utils/RSNetworkError";
import { HTTPMethods, RSEndpoint } from "../../config/endpoints";

/**
 * See prime-router/docs/api/check.yml for documentation
 */
export interface CheckSettingResult {
    result: "success" | "fail" | ""; // "" is client-side uninitialized state
    message: string;
}

/** parameters used for the request. Also used by the react page to make passing data down easier **/
export type CheckSettingParams = {
    orgName: string;
    receiverName: string;
};

export const useCheckSettingsCmd = () => {
    const { authorizedFetch } = useAuthorizedFetch<CheckSettingResult>();
    const checkSettingsCmd = new RSEndpoint({
        path: "/checkreceiver/org/:orgName/receiver/:receiverName",
        method: HTTPMethods.POST,
    });

    const updateValueSet = (params: CheckSettingParams) => {
        return authorizedFetch(checkSettingsCmd, {
            segments: { ...params },
        });
    };
    const mutation = useMutation<
        CheckSettingResult,
        RSNetworkError,
        CheckSettingParams
    >(updateValueSet);
    return {
        doCheck: mutation.mutateAsync,
        isLoading: mutation.isLoading,
        error: mutation.error,
    };
};
