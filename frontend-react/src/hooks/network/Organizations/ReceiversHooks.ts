import { useApiEndpoint } from "../UseApiEndpoint";
import {
    ReceiverApi,
    ReceiverListParams,
    RSReceiver,
} from "../../../network/api/Organizations/Receivers";

export const useReceiversList = (org: string) =>
    useApiEndpoint<ReceiverListParams, RSReceiver>(ReceiverApi, "list", "GET", {
        org: org,
    });
