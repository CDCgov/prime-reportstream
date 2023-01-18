import React from "react";

import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";
import { MemberType } from "../hooks/UseOktaMemberships";
import { AuthElement } from "../components/AuthElement";
import { withCatch } from "../components/RSErrorBoundary";

const Validate = () => {
    return withCatch(
        <FileHandler
            headingText="ReportStream File Validator"
            successMessage="File validated"
            handlerType={FileHandlerType.VALIDATION}
            resetText="Validate another file"
            submitText="Validate"
            showSuccessMetadata={false}
            showWarningBanner={false}
        />
    );
};

export default Validate;

export const ValidateWithAuth = () => (
    <AuthElement
        element={<Validate />}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
