import { useMutation } from "@tanstack/react-query";

import { messageTrackerEndpoints } from "../../../config/api/messageTracker";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";

// TODO: Convert to Query instead of mutation
export const useMessageSearch = () => {
    const { authorizedFetch } = useAuthorizedFetch<MessagesItem[]>();

    const messagesSearch = (messageId: string): Promise<MessagesItem[]> => {
        return authorizedFetch(messageTrackerEndpoints.search, {
            segments: { messageId },
            params: { messageId },
        });
    };

    const mutation = useMutation(messagesSearch);
    return {
        search: mutation.mutateAsync,
        isLoading: mutation.isLoading,
        error: mutation.error,
    };
};

// export const useMessageSearch = (messageId?: string) => {
//     return useRSQuery(
//         messageTrackerEndpoints.search,
//         {
//             params: {
//                 messageId,
//             },
//         },
//         {
//             enabled: !!messageId,
//         }
//     );
// };
