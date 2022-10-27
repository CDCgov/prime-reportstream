import { useMemo } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { createRequestConfig } from "../network/api/NewApi";
import OrganizationsAPI from "../network/api/OrganizationsApi";
import { SimpleError } from "../utils/UsefulTypes";

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
    const { activeMembership, oktaToken } = useSessionContext();
    /* Create a stable config reference with useMemo(). */
    const config = useMemo(
        () => {
            const { parsedName, service } = activeMembership || {};
            if (!service || !parsedName) {
                return new SimpleError("Missing sender or organization");
            }
            return createRequestConfig<{ org: string; sender: string }>(
                OrganizationsAPI,
                "sender",
                "GET",
                oktaToken?.accessToken,
                activeMembership?.parsedName,
                {
                    org: activeMembership?.parsedName || "",
                    sender: activeMembership?.service || "",
                }
            );
        },

        /* Note: we DO want to update config ONLY when these values update. If the linter
         * yells about a value you don't want to add, add an eslint-ignore comment. */
        [oktaToken?.accessToken, activeMembership]
    );

    /* Pass the stable config into the consumer and cast the response with types. */
    const {
        data: senderResponse,
        error,
        loading,
    } = useRequestConfig(config) as {
        data: Sender;
        error: string;
        loading: boolean;
    };

    // find the sender that matches the user's sender
    // (or just return the first one in the list)
    const sender = useMemo(() => {
        if (loading || error) {
            return null;
        }
        if (!senderResponse) {
            console.error(
                "No sender available for organization from API response"
            );
            return null;
        }
        if (!activeMembership?.service) {
            console.error("No sender available on active membership");
            return null;
        }
        return senderResponse;
    }, [senderResponse, activeMembership, error, loading]);

    /* Finally, return the values from the hook. */
    return {
        sender,
        error,
        loading,
    };
};
