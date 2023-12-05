import { useQuery } from "@tanstack/react-query";

import {
    HTTPMethods,
    RSApiEndpoints,
    RSEndpoint,
} from "../../../config/endpoints";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetch";

/** shape of data returned **/
export interface SettingRevision {
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
export type SettingRevisionParams = {
    orgId: string;
    settingType: "sender" | "receiver" | "organization";
};

/** endpoint component used below - not exported **/
const settingRevisionEndpoints: RSApiEndpoints = {
    getList: new RSEndpoint({
        path: "/waters/org/:orgId/settings/revs/:settingType",
        method: HTTPMethods.GET,
        queryKey: "orgSettingRevisions",
    }),
};

/** actual fetching component **/
export const useSettingRevisionEndpointsQuery = (
    params: SettingRevisionParams,
) => {
    const authorizedFetch = useAuthorizedFetch<SettingRevision[]>();

    // get all lookup tables in order to get metadata
    return useQuery({
        queryKey: ["history", params.orgId, params.settingType],
        queryFn: async () =>
            authorizedFetch(settingRevisionEndpoints.getList, {
                segments: params,
            }),
    });
};
