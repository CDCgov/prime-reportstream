import React from "react";
import { GridContainer } from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { MemberType } from "../../hooks/UseOktaMemberships";

export function ManagePublicKey() {
    return (
        <GridContainer className="padding-bottom-5 tablet:padding-top-6">
            <h1 className="margin-top-0 margin-bottom-5">Manage Public Key</h1>
            <p className="font-sans-md">
                Send your public key to begin the REST API authentication
                process.
            </p>
        </GridContainer>
    );
}

export const ManagePublicKeyWithAuth = () => (
    <AuthElement
        element={withCatchAndSuspense(<ManagePublicKey />)}
        requiredUserType={MemberType.SENDER}
    />
);
