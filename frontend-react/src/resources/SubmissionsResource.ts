import AuthResource from "./AuthResource";

export default class SubmissionsResource extends AuthResource {
    readonly organization: string | undefined = "";
    readonly id: string | undefined;
    readonly createdAt: Date | undefined;
    readonly reportItemCount: number = 0;
    readonly warningCount: number = 0;

    pk(params: any) {
        // For failed submissions, the report id will be null. Rest Hooks will not persist a record without a pk, thus
        // falling back to using createdAt.
        return this.id || this.createdAt?.toString();
    }

    /* INFO
       since we won't be using urlRoot to build our urls we still need to tell rest hooks
       how to uniquely identify this Resource

       >>> Kevin Haube, October 4, 2021
    */
    static get key() {
        return "SubmissionsResource";
    }

    static listUrl(searchParams: { organization: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/history/${searchParams.organization}/submissions`;
    }
}
