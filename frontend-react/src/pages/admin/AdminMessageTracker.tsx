import React from "react";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { MessageTracker } from "../../components/MessageTracker/MessageTracker";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

export function AdminMessageTracker() {
    return (
        <GridContainer>
            <Helmet>
                <title>Message Id Search</title>
            </Helmet>
            <article>
                <MessageTracker />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export function AdminMessageTrackerWithAuth() {
    return (
        <AuthElement
            element={withCatchAndSuspense(<AdminMessageTracker />)}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
