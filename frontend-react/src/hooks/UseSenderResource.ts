import { useMemo } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { createRequestConfig } from "../network/api/NewApi";
import OrganizationsAPI from "../network/api/OrganizationsApi";

import useRequestConfig from "./network/UseRequestConfig";

type SenderKeys = {
    scope: string;
    keys: {}[];
};

type Sender = {
    allowDuplicates: boolean;
    createdAt?: string;
    createdBy?: string;
    customerStatus: string;
    format: string;
    keys?: SenderKeys;
    name: string;
    organizationName: string;
    primarySubmissionMethod?: string;
    processingType: string;
    schemaName: string;
    senderType?: string;
    topic: string;
    version?: number;
};

export const useSenderResource = () => {
    /* Access the session. */
    const { memberships, oktaToken } = useSessionContext();
    /* Create a stable config reference with useMemo(). */
    const config = useMemo(
        () =>
            createRequestConfig<{ org: string }>(
                OrganizationsAPI,
                "senders",
                "GET",
                oktaToken?.accessToken,
                memberships.state.active?.parsedName,
                {
                    org: memberships.state.active?.parsedName || "",
                }
            ),
        /* Note: we DO want to update config ONLY when these values update. If the linter
         * yells about a value you don't want to add, add an eslint-ignore comment. */
        [oktaToken?.accessToken, memberships.state.active]
    );

    /* Pass the stable config into the consumer and cast the response with types. */
    const {
        data: senders,
        error,
        loading,
    } = useRequestConfig(config) as {
        data: Sender[];
        error: string;
        loading: boolean;
    };

    // find the sender that matches the user's sender
    // (or just return the first one in the list)
    const sender = useMemo(() => {
        if (!senders || !senders.length) {
            console.error(
                "No sender available for organization from API response"
            );
            return null;
        }
        console.log("!!! memberships", memberships);
        if (!memberships?.state?.active?.senderName) {
            console.error("No sender available on active membership");
            return null;
        }
        const matchedSender = senders.find(
            (possibleSender: Sender) =>
                possibleSender.name === memberships?.state?.active?.senderName
        );
        if (!matchedSender) {
            console.error(
                `No senders match: ${memberships?.state?.active?.senderName}`
            );
            return null;
        }
        return matchedSender;
    }, [senders, memberships.state.active]);

    /* Finally, return the values from the hook. */
    return {
        sender,
        error,
        loading,
    };
};
