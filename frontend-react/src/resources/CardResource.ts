import config from "../config";

import AuthResource from "./AuthResource";

const { RS_API_URL } = config;

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

    static urlRoot = `${RS_API_URL}/api/history/summary/tests`;
}
