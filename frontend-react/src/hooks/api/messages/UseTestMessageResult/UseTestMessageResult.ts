// hooks/api/messages/UseSubmitTestMessage/UseSubmitTestMessage.ts
import { useMutation } from "@tanstack/react-query";
import { useCallback } from "react";
import { useParams } from "react-router";
import { reportsEndpoints, RSMessage, RSMessageResult } from "../../../../config/endpoints/reports";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { Organizations } from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { testResult } = reportsEndpoints;

interface TestMessageVariables {
    body: RSMessage | null;
}

function useSubmitTestMessage() {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const { orgname, receivername } = useParams();
    const parsedName = activeMembership?.parsedName;
    const isAdmin = Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;

    const mutationFn = useCallback(
        async ({ body }: TestMessageVariables) => {
            /* Guard-rail so non-admin callers never hit the endpoint */
            if (!isAdmin) throw new Error("Not authorized to test messages");

            return authorizedFetch<RSMessageResult>(
                {
                    params: {
                        receiverName: receivername,
                        organizationName: orgname,
                    },
                    data: body,
                },
                testResult,
            );
        },
        [authorizedFetch, orgname, receivername, isAdmin],
    );

    const mutation = useMutation<RSMessageResult, Error, TestMessageVariables>({
        mutationFn,
    });

    return {
        ...mutation,
        isDisabled: !isAdmin,
    };
}

export default useSubmitTestMessage;
