import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

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
