import React from "react";
import { Helmet } from "react-helmet";

// import SigningForm from "./SigningForm";
import SuccessPage from "./SuccessPage";

function SignTermsOfService() {
    return (
        <>
            <Helmet>
                <title>Sign the Terms of Service</title>
            </Helmet>
            <div className="grid-container">
                <SuccessPage />
            </div>
        </>
    );
}

export default SignTermsOfService;
