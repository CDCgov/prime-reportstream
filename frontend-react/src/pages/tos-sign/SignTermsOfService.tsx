import React, { useState } from "react";
import { Helmet } from "react-helmet";

import SigningForm from "./SigningForm";
import SuccessPage from "./SuccessPage";

function SignTermsOfService() {
    const [signed, setSigned] = useState(false);
    const signedCallback = () => {
        setSigned(true);
    };

    return (
        <>
            <Helmet>
                <title>Sign the Terms of Service</title>
            </Helmet>
            <div className="grid-container">
                {signed ? (
                    <SuccessPage />
                ) : (
                    <SigningForm signedCallback={signedCallback} />
                )}
            </div>
        </>
    );
}

export default SignTermsOfService;
