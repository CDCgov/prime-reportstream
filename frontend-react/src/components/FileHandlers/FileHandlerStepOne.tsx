import React, { useRef } from "react";
import { FileInputRef, Dropdown, Button } from "@trussworks/react-uswds";

import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";
import { FileType } from "../../hooks/UseFileHandler";

export interface FileHandlerFormProps {
    fileType?: FileType;
    schemaOptions: SchemaOption[];
    selectedSchemaOption: SchemaOption | null;
    onSchemaChange: (schemaOption: SchemaOption | null) => void;
    handleNextFileHandlerStep: () => void;
}

export const FileHandlerStepOne = ({
    fileType,
    schemaOptions,
    selectedSchemaOption,
    onSchemaChange,
    handleNextFileHandlerStep,
}: FileHandlerFormProps) => {
    const fileInputRef = useRef<FileInputRef>(null);

    return (
        <div className="grid-col flex-1 display-flex flex-column">
            <p>Select data model</p>
            <Dropdown
                id="upload-schema-select"
                name="upload-schema-select"
                value={selectedSchemaOption?.value || ""}
                onChange={(e) => {
                    const option =
                        schemaOptions.find(
                            ({ value }) => value === e.target.value
                        ) || null;

                    if (option?.format !== fileType) {
                        fileInputRef.current?.clearFiles();
                    }

                    onSchemaChange(option);
                }}
            >
                <option value="">- Select -</option>
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
