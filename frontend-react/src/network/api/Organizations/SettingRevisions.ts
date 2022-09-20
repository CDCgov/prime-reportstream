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

/** type for api parameters passed to server **/
interface SettingRevisionParams {
    orgname: string;
    settingtype: "sender" | "receiver" | "organization";
}

/** endpoint component used below **/
const settingRevisionEndpoints: RSApiEndpoints = {
    getList: new RSEndpoint({
        path: "/waters/org/:org/settings/revs/:settingtype",
        method: HTTPMethods.GET,
    }),
};

/** actual fetching component **/
export const useRevisionEndpointsQuery = (params: SettingRevisionParams) => {
    const fetchFn = useAuthorizedFetch<SettingRevision[]>();

    // get all lookup tables in order to get metadata
    return useQuery<SettingRevision[]>(["org", "settingtype"], () =>
        fetchFn(settingRevisionEndpoints.getList, {
            segments: params as StringIndexed,
        })
    );
};
