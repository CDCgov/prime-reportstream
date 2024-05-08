import { useMutation } from "@tanstack/react-query";

import { HTTPMethods, RSEndpoint } from "../../../../config/endpoints";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { RSNetworkError } from "../../../../utils/RSNetworkError";

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

const useCheckOrganizationReceiver = () => {
    const { authorizedFetch } = useSessionContext();
    const checkSettingsCmd = new RSEndpoint({
        path: "/checkreceiver/org/:orgName/receiver/:receiverName",
        method: HTTPMethods.POST,
        queryKey: "checkReceiverSettings",
    });

    const updateValueSet = (params: CheckSettingParams) => {
        return authorizedFetch<CheckSettingResult>(
            {
                segments: { ...params },
            },
            checkSettingsCmd,
        );
    };
    return useMutation<CheckSettingResult, RSNetworkError, CheckSettingParams>({
        mutationFn: updateValueSet,
    });
};

export default useCheckOrganizationReceiver;
