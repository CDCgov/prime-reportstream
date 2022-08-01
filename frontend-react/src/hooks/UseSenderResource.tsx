import { useMemo } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { createRequestConfig } from "../network/api/NewApi";
import OrganizationsAPI from "../network/api/OrganizationsApi";

import useRequestConfig from "./network/UseRequestConfig";

type Sender = {
    allowDuplicates: boolean;
    createdAt?: string;
    createdBy?: string;
    customerStatus: string;
    format: string;
    keys?: null;
    name: string;
    organizationName: string;
    primarySubmissionMethod?: null;
    processingType: string;
    schemaName: string;
    senderType?: null;
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
        data: any; // TODO (#5892): Should return Newable object or array of Newable objects.
        error: string;
        loading: boolean;
    };

    const sender = useMemo(() => {
        if (!senders || !senders.length) {
            return senders;
        }
        console.log("!!! membersshiop", memberships);
        if (!memberships?.state?.active?.senderName) {
            return senders[0];
        }
        return senders.find(
            (possibleSender) =>
                possibleSender.name === memberships?.state?.active?.senderName
        );
    }, [senders, memberships.state.active]);
    /* Finally, return the values from the hook. */
    return {
        sender,
        error,
        loading,
    };
};
