import AuthResource from "./AuthResource";

interface SettingRevision {
    id: number;
    name: string;
    version: number;
    createdAt: string;
    createdBy: string;
    settingJson: string;
}

export type SettingRevisionParams = {
    orgname: string;
    settingtype: "sender" | "receiver" | "organization";
};

export default class OrganizationResource
    extends AuthResource
    implements SettingRevision
{
    id = 0;
    name = "";
    version = 0;
    createdAt = "";
    createdBy = "";
    settingJson = "[]";

    pk() {
        return `OrganizationResource-${this.id}`;
    }

    static get key() {
        return "OrganizationResource";
    }

    static listUrl(params: SettingRevisionParams): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/waters/org/${params.orgname}/settings/revs/${params.settingtype}`;
    }
}
