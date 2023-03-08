import { ResponseError } from "../../config/endpoints/waters";
import { Destination } from "../../resources/ActionDetailsResource";

import {
    FileQualityFilterDisplay,
    FileSuccessDisplay,
    RequestedChangesDisplay,
    RequestLevel,
} from "./FileHandlerMessaging";

interface FileHandlerStepThreeProps {
    destinations?: string;
    errorMessaging: { message: string; heading: string };
    errors: ResponseError[];
    hasQualityFilterMessages: boolean;
    isFileSuccess: boolean;
    qualityFilterMessages: Destination[] | undefined;
    reportId?: string;
    successDescription: string;
    successTimestamp?: string;
    warnings: ResponseError[];
}

export const FileHandlerStepThree = ({
    destinations,
    errorMessaging,
    errors,
    hasQualityFilterMessages,
    isFileSuccess,
    qualityFilterMessages,
    reportId,
    successDescription,
    successTimestamp,
    warnings,
}: FileHandlerStepThreeProps) => {
    return (
        <>
            {isFileSuccess && warnings.length === 0 && (
                <FileSuccessDisplay
                    extendedMetadata={{
                        destinations,
                        timestamp: successTimestamp,
                        reportId,
                    }}
                    heading="Validate another file"
                    message={successDescription}
                    showExtendedMetadata={false}
                />
            )}
            {warnings.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.WARNING}
                    data={warnings}
                    message="The following warnings were returned while processing your file. We recommend addressing warnings to enhance clarity."
                    heading="File validated with recommended edits"
                />
            )}
            {errors.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.ERROR}
                    data={errors}
                    message={errorMessaging.message}
                    heading={errorMessaging.heading}
                />
            )}
            {hasQualityFilterMessages && (
                <FileQualityFilterDisplay
                    destinations={qualityFilterMessages}
                    heading=""
                    message={`The file does not meet the jurisdiction's schema. Please resolve the errors below.`}
                />
            )}
        </>
    );
};
