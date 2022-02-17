import { Api } from "./api/Api";

export interface Endpoint {
    url: string;
    api: typeof Api;
}