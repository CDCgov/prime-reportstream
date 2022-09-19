import { API } from "../NewApi";

export interface ISettingRevision {
    id: number;
    name: string;
    version: number;
    createdAt: string;
    createdBy: string;
    settingJson: string;
}

interface ISettingRevisionParams {
    orgname: string;
    settingtype: "sender" | "receiver" | "organization";
}

/** A class representing a Receiver object from the API */
export class SettingRevision implements ISettingRevision {
    id: number = 0;
    name: string = "";
    version: number = 0;
    createdAt: string = "";
    createdBy: string = "";
    settingJson: string = "[]";

    constructor(args: Partial<ISettingRevision>) {
        Object.assign(this, args);
    }
}

export const SettingRevisionApi = new API(
    SettingRevision,
    "/api/waters"
).addEndpoint("list", "/org/:org/settings/revs/:settingtype", ["GET"]);
