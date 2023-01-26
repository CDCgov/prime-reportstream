import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

/** endpoint component used below - not exported **/
export const settingRevisionEndpoints = {
    settingRevisions: new RSEndpoint({
        path: "/waters/org/:org/settings/revs/:settingType",
        methods: {
            [HTTPMethods.GET]: {} as SettingRevision[],
        },
        params: {} as SettingRevisionParams,
        queryKey: "orgSettingRevisions",
    } as const),
};
