import { useApiEndpoint } from "../UseApiEndpoint";
import {
    ReceiverApi,
    ReceiverListParams,
    RSReceiver,
} from "../../../network/api/Organizations/Receivers";

/** Retrieves a list of Receivers for an org from the API
 * > **This call requires the use of `trigger()`**
 *
 * @param org {string?} The user's active memebership `parsedName` */
export const useReceiversList = (org?: string) =>
    // Uses Partial<T> because we require the trigger and conditionally call
    useApiEndpoint<Partial<ReceiverListParams>, RSReceiver[]>(
        ReceiverApi,
        "list",
        "GET",
        { org: org },
        { requireTrigger: true }
    );
