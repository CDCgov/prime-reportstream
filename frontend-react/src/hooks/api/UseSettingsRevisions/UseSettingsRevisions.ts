import { useSuspenseQuery } from "@tanstack/react-query";

import {
    HTTPMethods,
    RSApiEndpoints,
    RSEndpoint,
} from "../../../config/endpoints";
import useSessionContext from "../../../contexts/Session/useSessionContext";

/** shape of data returned **/
export interface RSSettingRevision {
    id: number;
    name: string;
    version: number;
    createdAt: string;
    createdBy: string;
    isDeleted: boolean;
    isActive: boolean;
    settingJson: string;
}

/** parameters used for the request. Also used by the react page to make passing data down easier **/
export interface RSSettingRevisionParams {
    org: string;
    settingType: "sender" | "receiver" | "organization";
}

export type RSSettingRevisionParamsRecord = RSSettingRevisionParams &
    Record<string, string>;

/** endpoint component used below - not exported **/
const settingRevisionEndpoints: RSApiEndpoints = {
    getList: new RSEndpoint({
        path: "/waters/org/:org/settings/revs/:settingType",
        method: HTTPMethods.GET,
        queryKey: "orgSettingRevisions",
    }),
};

/** actual fetching component **/
const useSettingsRevision = (params: RSSettingRevisionParamsRecord) => {
    const { authorizedFetch } = useSessionContext();

    // get all lookup tables in order to get metadata
    return useSuspenseQuery({
        queryKey: ["history", params.org, params.settingType],
        queryFn: async () =>
            authorizedFetch<RSSettingRevision[]>(
                {
                    segments: params,
                },
                settingRevisionEndpoints.getList,
            ),
    });
};

export default useSettingsRevision;
