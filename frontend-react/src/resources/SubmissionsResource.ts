import AuthResource from "./AuthResource";

type SubmissionsResourceParams = {
    organization: string;
    pageSize: number;
    cursor: string;
    sort: string;
};

const FALLBACKDATE = "2020-01-01T00:00:00.000Z";

export default class SubmissionsResource extends AuthResource {
    readonly taskId: number = 0;
    readonly createdAt: Date = new Date(FALLBACKDATE);
    readonly sendingOrg: string = "";
    readonly httpStatus: number = 0;
    readonly externalName: string = "";
    readonly id: string | undefined;
    readonly topic: string = "";
    readonly reportItemCount: number = 0;
    readonly warningCount: number = 0;
    readonly errorCount: number = 0;

    pk() {
        // For failed submissions, the report id will be null. Rest Hooks will not cache a record without a pk, thus
        // falling back to using createdAt.
        return this.id || this.createdAt?.toString();
    }

    static get key() {
        return "SubmissionsResource";
    }

    static listUrl(searchParams: SubmissionsResourceParams): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/history/${searchParams.organization}/submissions?pagesize=${searchParams.pageSize}&cursor=${searchParams.cursor}&sort=${searchParams.sort}`;
    }

    isSuccessSubmitted(): boolean {
        return this.id !== null;
    }
}
