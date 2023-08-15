import {
    HTTPMethods,
    RSApiEndpoints,
    RSEndpoint,
} from "../../../config/endpoints";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";

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
export const useSettingRevisionEndpointsQuery = (
    params: SettingRevisionParams,
) => {
    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<SettingRevision[]>();

    // get all lookup tables in order to get metadata
    return rsUseQuery(["history", params.org, params.settingType], async () =>
        authorizedFetch(settingRevisionEndpoints.getList, {
            segments: params,
        }),
    );
};
