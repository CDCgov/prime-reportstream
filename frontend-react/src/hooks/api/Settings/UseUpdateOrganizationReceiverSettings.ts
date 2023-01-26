import { settingsEndpoints } from "../../../config/api/settings";
import { useRSMutation } from "../UseRSQuery";

// TODO: Change workflow so that a Receiver object is given and
// then stringified before transport (using its properties for segments)
export const useUpdateOrganizationReceiverSettings = (orgName: string) => {
    return useRSMutation(
        settingsEndpoints.receiver,
        "PUT",
        ({ receiverName, data }: { receiverName: string; data: string }) => {
            return {
                segments: {
                    orgName,
                    receiverId: receiverName,
                },
                data,
            };
        }
    );
};
