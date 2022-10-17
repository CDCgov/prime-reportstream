import React from "react";

import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";
import { MemberType } from "../hooks/UseOktaMemberships";
import { AuthElement } from "../components/AuthElement";

import { FeatureFlagName } from "./misc/FeatureFlags";

const UploadToPipeline = () => {
    return (
        <FileHandler
            headingText="File Uploader"
            successMessage="Your file has been uploaded"
            handlerType={FileHandlerType.UPLOAD}
            resetText="Upload another file"
            submitText="Upload"
            showSuccessMetadata={true}
            showWarningBanner={true}
            warningText={
                "Uploading files on this page will result in data being transmitted to public health authorities. Use caution when uploading data."
            }
        />
    );
};

export default UploadToPipeline;

export const UploadToPipelineWithAuth = () => (
    <AuthElement
        element={<UploadToPipeline />}
        requiredUserType={MemberType.PRIME_ADMIN}
        requiredFeatureFlag={FeatureFlagName.USER_UPLOAD}
    />
);
