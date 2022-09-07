import AuthResource from "./AuthResource";

export default class AdmAction extends AuthResource {
    /* the unique id  */
    readonly actionId: number = 0; // BigInt
    readonly actionName: string = "";
    readonly createdAt: string = "";
    readonly httpStatus: number = -1;
    readonly actionParams: string = "";
    readonly actionResult: string = "";
    readonly actionResponse: string = "";
    readonly contentLength: number = 0;
    readonly senderIp: string = "";
    readonly sendingOrg: string = "";
    readonly sendingOrgClient: string = "";
    readonly externalName: string = "";
    readonly username: string = "";

    pk() {
        return `actionid-${this.actionId}}`;
    }

    static get key() {
        return "AdmActionResource";
    }

    static urlRoot = `${process.env.REACT_APP_BACKEND_URL}/api/adm/getresend`;

    static url(params: { daysToShow: number }): string {
        return `${this.urlRoot}?days_to_show=${params.daysToShow}`;
    }

    filterMatch(search: string | null): boolean {
        if (!search) {
            return true; // no search returns EVERYTHING
        }
        // combine all elements to be searched.
        return `${this.actionParams} ${this.actionResponse}`
            .toLowerCase()
            .includes(`${search.toLowerCase()}`);
    }
}
