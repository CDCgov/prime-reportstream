import {
    settingRevisionEndpoints,
    SettingRevisionParams,
} from "../../config/api/revisions";

import { useRSQuery } from "./UseRSQuery";

// TODO use arg array for function parameters
/** actual fetching component **/
export const useSettingRevision = ({
    org,
    settingType,
}: SettingRevisionParams) => {
    return useRSQuery(settingRevisionEndpoints.settingRevisions, {
        segments: { org, settingType },
    });
};
