import { useMutation } from "@tanstack/react-query";

import {
    MessageListResource,
    messageTrackerEndpoints,
} from "../../../../config/endpoints/messageTracker";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { search } = messageTrackerEndpoints;

/** Hook consumes the MessageTrackerApi "search" endpoint and delivers the response **/
const useMessageSearch = () => {
    const { authorizedFetch } = useSessionContext();

    const messagesSearch = (
        messageId: string,
    ): Promise<MessageListResource[]> => {
        return authorizedFetch<MessageListResource[]>(
            {
                segments: { messageId },
                params: { messageId },
            },
            search,
        );
    };

    return useMutation({ mutationFn: messagesSearch });
};

export default useMessageSearch;
