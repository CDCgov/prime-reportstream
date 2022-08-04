/** @remarks For later use with runtime type checking API data */
import { API } from "./NewApi";

export enum Endpoints {
    DETAIL = "detail",
}
/** A class representing an Organization object from the API */
export class RSOrganization {}

export interface ReceiversUrlVars {
    org: string;
}
/**
 * Contains the API information to get RSReceivers from the API
 * 1. Resource: {@link RSOrganization}
 * 2. Endpoints:
 *      <ul>
 *          <li>"detail" -> Fetches details for a single org </li>
 *      </ul>
 */
const OrganizationApi = new API(
    RSOrganization,
    "/api/settings/organizations"
).addEndpoint(Endpoints.DETAIL, "/:org", ["GET"]);

export default OrganizationApi;
