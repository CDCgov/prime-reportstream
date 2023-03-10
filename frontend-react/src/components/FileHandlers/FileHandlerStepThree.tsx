import { ResponseError } from "../../config/endpoints/waters";
import { Destination } from "../../resources/ActionDetailsResource";
import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";

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
    hasQualityFilterMessages: boolean | undefined;
    isFileSuccess: string | boolean;
    qualityFilterMessages: Destination[] | undefined;
    reportId?: string;
    successDescription: string;
    successTimestamp?: string;
    warnings: ResponseError[];
    selectedSchemaOption: SchemaOption | null;
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
    selectedSchemaOption,
}: FileHandlerStepThreeProps) => {
    return (
        <div className="file-handler-table">
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
            {errors.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.ERROR}
                    data={errors}
                    message={errorMessaging.message}
                    heading={errorMessaging.heading}
                    schemaColumnHeader={selectedSchemaOption.format}
                />
            )}
            {warnings.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.WARNING}
                    data={warnings}
                    message="To avoid problems when sending files later, we strongly recommend fixing these issues now."
                    heading="Recommended edits found"
                    schemaColumnHeader={selectedSchemaOption.format}
                />
            )}

            {hasQualityFilterMessages && (
                <FileQualityFilterDisplay
                    destinations={qualityFilterMessages}
                    heading=""
                    message={`The file does not meet the jurisdiction's schema. Please resolve the errors below.`}
                />
            )}
        </div>
    );
};
