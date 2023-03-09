import React from "react";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { MemberType } from "../../hooks/UseOktaMemberships";

export function ManagePublicKey() {
    return (
        <div className="grid-container margin-bottom-5 tablet:margin-top-6">
            <div>
                <h1 className="margin-top-0 margin-bottom-5">
                    Manage Public Key
                </h1>
                <p className="font-sans-md">
                    Send your public key to begin the REST API authentication
                    process.
                </p>
            </div>
            y
        </div>
    );
}

export const ManagePublicKeyWithAuth = () => (
    <AuthElement
        element={withCatchAndSuspense(<ManagePublicKey />)}
        requiredUserType={MemberType.SENDER}
    />
);
