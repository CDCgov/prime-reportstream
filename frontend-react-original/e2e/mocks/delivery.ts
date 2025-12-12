import { addDays } from "date-fns";

export const MOCK_GET_DELIVERY = {
    deliveryId: 4174622,
    batchReadyAt: "2024-04-04T15:19:19.983Z",
    expires: addDays(new Date(), 7),
    receiver: "ignore.BLOBSTORE",
    receivingOrgSvcStatus: null,
    reportId: "73e3cbc8-9920-4ab7-871f-843a1db4c074",
    topic: "covid-19",
    reportItemCount: 5,
    fileName: "hhsprotect-covid-19-73e3cbc8-9920-4ab7-871f-843a1db4c074.csv",
    fileType: "CSV",
};
