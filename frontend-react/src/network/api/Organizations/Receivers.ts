import { API } from "../NewApi";

export interface RSReceiverInterface {
    name: string;
    organizationName: string;
}
/** A class representing a Receiver object from the API */
export class RSReceiver implements RSReceiverInterface {
    name: string = "";
    organizationName: string = "";

    constructor(args: Partial<RSReceiverInterface>) {
        Object.assign(this, args);
    }
}
/**
 * Contains the API information to get RSReceivers from the API
 * 1. Resource: {@link RSReceiver}
 * 2. Endpoints:
 *      <ul>
 *          <li>"list" -> A list of receivers for an organization</li>
 *      </ul>
 */
export const ReceiverApi = new API(
    RSReceiver,
    "/api/settings/organizations"
).addEndpoint("list", "/:org/receivers", ["GET"]);

export interface ReceiverDetailParams {
    org: string;
    receiver: string;
}
/* Just `org: string` */
export type ReceiverListParams = Omit<ReceiverDetailParams, "receiver">;
