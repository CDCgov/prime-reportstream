import React, { useRef } from "react";
import { FileInputRef, Dropdown, Button } from "@trussworks/react-uswds";

import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";
import { FileType } from "../../utils/TemporarySettingsAPITypes";

export interface FileHandlerFormProps {
    fileType?: FileType;
    handleNextFileHandlerStep: () => void;
    onSchemaChange: (schemaOption: SchemaOption) => void;
    schemaOptions: SchemaOption[];
    selectedSchemaOption: SchemaOption;
}

export const FileHandlerStepOne = ({
    fileType,
    handleNextFileHandlerStep,
    onSchemaChange,
    schemaOptions,
    selectedSchemaOption,
}: FileHandlerFormProps) => {
    const fileInputRef = useRef<FileInputRef>(null);

    return (
        <div className="grid-col flex-1 display-flex flex-column">
            <p>Select data model</p>
            <Dropdown
                id="upload-schema-select"
                name="upload-schema-select"
                value={selectedSchemaOption.value}
                onChange={(e) => {
                    const option = schemaOptions.find(
                        ({ value }: SchemaOption) => value === e.target.value
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
                {schemaOptions.map(({ title, value }) => (
                    <option key={value} value={value}>
                        {title}
                    </option>
                ))}
            </Dropdown>
            <Button
                disabled={!selectedSchemaOption?.value?.length}
                className="usa-button flex-align-self-start height-5 margin-top-4"
                type={"submit"}
                onClick={() => handleNextFileHandlerStep()}
            >
                Continue
            </Button>
        </div>
    );
};
