import { useRef } from "react";
import { FileInputRef, Dropdown, Button } from "@trussworks/react-uswds";

import useSenderSchemaOptions, {
    SchemaOption,
} from "../../senders/hooks/UseSenderSchemaOptions";
import Spinner from "../Spinner";
import { FileHandlerStepProps } from "../../pages/file-handler/FileHandler";

import FileHandlerPiiWarning from "./FileHandlerPiiWarning";

export interface FileHandlerSchemaSelectionStepProps
    extends FileHandlerStepProps {
    onSchemaChange: (schemaOption: SchemaOption) => void;
}

export interface FileHandlerSchemaSelectionStepBaseProps
    extends FileHandlerSchemaSelectionStepProps {
    schemas: SchemaOption[];
}

export function FileHandlerSchemaSelectionStepBase({
    fileType,
    isValid,
    selectedSchemaOption,
    onSchemaChange,
    onNextStepClick,
    schemas,
}: FileHandlerSchemaSelectionStepBaseProps) {
    const fileInputRef = useRef<FileInputRef>(null);

    return (
        <div>
            <FileHandlerPiiWarning />

            <p className="margin-top-4 margin-bottom-2">Select data model</p>

            <div className="margin-bottom-4">
                <Dropdown
                    id="upload-schema-select"
                    name="upload-schema-select"
                    value={selectedSchemaOption.value}
                    onChange={(e) => {
                        const option = schemas.find(
                            ({ value }: SchemaOption) =>
                                value === e.target.value,
                        )!;

                        if (option?.format !== fileType) {
                            fileInputRef.current?.clearFiles();
                        }

                        onSchemaChange(option);
                    }}
                >
                    <option value="" disabled>
                        - Select -
                    </option>
                    {schemas.map(({ title, value }, index) => (
                        <option key={index} value={value}>
                            {title}
                        </option>
                    ))}
                </Dropdown>
            </div>

            <Button
                disabled={!isValid}
                className="usa-button"
                type="submit"
                onClick={onNextStepClick}
            >
                Continue
            </Button>
        </div>
    );
}

export default function FileHandlerSchemaSelectionStep(
    props: FileHandlerSchemaSelectionStepProps,
) {
    const { data: schemas, isLoading } = useSenderSchemaOptions();
    if (isLoading) {
        return (
            <div>
                <Spinner />

                <div className="text-center">Loading...</div>
            </div>
        );
    }
    return <FileHandlerSchemaSelectionStepBase {...props} schemas={schemas} />;
}
