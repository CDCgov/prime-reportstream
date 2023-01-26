import { settingRevisionEndpoints } from "../../../config/api/revisions";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";

/** parameters used for the request. Also used by the react page to make passing data down easier **/
export type SettingRevisionParams = {
    org: string;
    settingType: "sender" | "receiver" | "organization";
};

/** actual fetching component **/
export const useSettingRevisionEndpointsQuery = (
    params: SettingRevisionParams
) => {
    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<SettingRevision[]>();

    // get all lookup tables in order to get metadata
    return rsUseQuery(["org", "settingType"], async () =>
        authorizedFetch(settingRevisionEndpoints.settingRevisions, {
            segments: params,
        })
    );
};
