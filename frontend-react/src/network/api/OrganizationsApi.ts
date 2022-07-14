/** @remarks For later use with runtime type checking API data */
import { API } from "./NewApi";

export enum Endpoints {
    DETAIL = "detail",
    ALL_RECEIVERS = "receivers",
}

export class RSOrganization {}
/** @deprecated For compile-time type checks while #5892 is worked on */
export interface RSOrganizationInterface {
    name: string;
}

export interface ReceiversUrlVars {
    org: string;
}

const OrganizationApi = new API(RSOrganization, "/api/settings/organizations")
    .addEndpoint(Endpoints.DETAIL, "/:org", ["GET"])
    .addEndpoint(Endpoints.ALL_RECEIVERS, "/:org/receivers", ["GET"]);

export default OrganizationApi;
