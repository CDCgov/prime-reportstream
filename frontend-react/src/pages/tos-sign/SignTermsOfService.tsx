import React from "react";
import { Helmet } from "react-helmet";

import SigningForm from "./SigningForm";

function SignTermsOfService() {
    return (
        <>
            <Helmet>
                <title>Sign the Terms of Service</title>
            </Helmet>
            <div className="grid-container">
                <SigningForm />
            </div>
        </>
    );
}

export default SignTermsOfService;
