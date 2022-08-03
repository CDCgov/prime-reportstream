import { API } from "../NewApi";

export class RSReceiver {
    name: string = "";
    organizationName: string = "";
}
export const ReceiverApi = new API(RSReceiver, "/api/settings/organizations")
    .addEndpoint("list", "/:org/receivers", ["GET"])
    .addEndpoint("detail", "/:org/receivers/:receiver", ["GET"]);

export interface ReceiverDetailParams {
    org: string;
    receiver: string;
}
/* Just `org: string` */
export type ReceiverListParams = Omit<ReceiverDetailParams, "receiver">;
