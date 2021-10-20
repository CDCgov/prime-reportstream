import React, { useState } from "react";
import { Helmet } from "react-helmet";

import SigningForm, { AgreementBody } from "./SigningForm";
import SuccessPage from "./SuccessPage";

function SignTermsOfService() {
    const [signed, setSigned] = useState(false);
    const [data, setData] = useState({
        title: "string",
        firstName: "string",
        lastName: "string",
        email: "string",
        territory: "string",
        organizationName: "string",
        operatesInMultipleStates: false,
        agreedToTermsOfService: false,
    })
    const signedCallback = (data: AgreementBody) => {
        setData(data)
        setSigned(true);
        console.log(`From parent: \n${JSON.stringify(data)}`)
    };

    return (
        <>
            <Helmet>
                <title>Sign the Terms of Service</title>
            </Helmet>
            <div className="grid-container">
                {signed ? (
                    <SuccessPage data={data} />
                ) : (
                    <SigningForm signedCallback={signedCallback} />
                )}
            </div>
        </>
    );
}

export default SignTermsOfService;
