import { Button, ButtonGroup } from "@trussworks/react-uswds";
import React, { useState } from "react";
import { Helmet } from "react-helmet";

import SigningForm from "./SigningForm";

// import SigningForm from "./SigningForm";
import SuccessPage from "./SuccessPage";

function SignTermsOfService() {
    /* TODO: REMOVE AFTER DEV */
    const content = {
        signing: <SigningForm />,
        success: <SuccessPage />,
    };
    const [page, setPage] = useState(content.signing);
    const handleClick = (e: any) => {
        e.preventDefault();
        if (e.target.textContent.toLowerCase() === "success")
            return setPage(content.success);
        return setPage(content.signing);
    };
    /* TODO: END REMOVE */

    return (
        <>
            <Helmet>
                <title>Sign the Terms of Service</title>
            </Helmet>
            <div className="grid-container">
                {/* TODO: REMOVE AFTER DEV*/}
                <ButtonGroup>
                    <Button onClick={handleClick} type="button">
                        Signing
                    </Button>
                    <Button onClick={handleClick} type="button">
                        Success
                    </Button>
                </ButtonGroup>
                {/* TODO: END MOVE */}

                {page}
            </div>
        </>
    );
}

export default SignTermsOfService;
