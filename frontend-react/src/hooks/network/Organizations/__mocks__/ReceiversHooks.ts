import * as ReceiverHooks from "../ReceiversHooks";
import { RSReceiver } from "../../../../network/api/Organizations/Receivers";

export const mockReceiverHook = jest.spyOn(ReceiverHooks, "useReceiversList");
/** TEST UTILITY - generates `RSReceiver[]`, each with a unique `name` (starting from "elr-0")
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
