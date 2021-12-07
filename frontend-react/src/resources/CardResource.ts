import AuthResource from "./AuthResource";

export default class CardResource extends AuthResource {
    readonly id: string = "cardId";
    readonly title: string = "title";
    readonly subtitle: string = "subtitle";
    readonly daily: number = 0;
    readonly last: number = 0;
    readonly positive: boolean = false;
    readonly change: number = 0;
    readonly data: number[] = [];

    pk() {
        return this.id;
    }

    static urlRoot = `${process.env.REACT_APP_BACKEND_URL}/api/history/summary/tests`;
}
