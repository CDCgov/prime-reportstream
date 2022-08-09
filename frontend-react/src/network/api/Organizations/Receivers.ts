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
/** TEST UTILITY - generates `RSReceiver[]`, each with a unique `name` (starting from "elr-0")
 *
 * @param count {number} How many unique receivers you want. */
export const receiversGenerator = (count: number) => {
    const receivers: RSReceiver[] = [];
    for (let i = 0; i < count; i++) {
        receivers.push(
            new RSReceiver({ name: `elr-${i}`, organizationName: "testOrg" })
        );
    }
    return receivers;
};
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
