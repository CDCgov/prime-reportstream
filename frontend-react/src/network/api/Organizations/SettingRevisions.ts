import { useQuery } from "@tanstack/react-query";

import {
    HTTPMethods,
    RSApiEndpoints,
    RSEndpoint,
} from "../../../config/endpoints";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { StringIndexed } from "../../../utils/UsefulTypes";

/** shape of data returned **/
export interface SettingRevision {
    id: number;
    name: string;
    version: number;
    createdAt: string;
    createdBy: string;
    settingJson: string;
}

/** parameters used for the request. Also used by the react page to make passing data down easier **/
export type SettingRevisionParams = {
    org: string;
    settingType: "sender" | "receiver" | "organization";
};

/** endpoint component used below - not exported **/
const settingRevisionEndpoints: RSApiEndpoints = {
    getList: new RSEndpoint({
        path: "/waters/org/:org/settings/revs/:settingType",
        method: HTTPMethods.GET,
    }),
};

/** actual fetching component **/
export const useRevisionEndpointsQuery = (params: SettingRevisionParams) => {
    const fetchFn = useAuthorizedFetch<SettingRevision[]>();

    // get all lookup tables in order to get metadata
    return useQuery<SettingRevision[]>(["org", "settingType"], () =>
        fetchFn(settingRevisionEndpoints.getList, {
            segments: params as StringIndexed,
        })
    );
};
