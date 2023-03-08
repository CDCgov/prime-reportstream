import React, { useRef } from "react";
import { FileInputRef, Dropdown } from "@trussworks/react-uswds";

import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";
import { FileType } from "../../hooks/UseFileHandler";

export interface FileHandlerFormProps {
    fileType?: FileType;
    schemaOptions: SchemaOption[];
    selectedSchemaOption: SchemaOption | null;
    onSchemaChange: (schemaOption: SchemaOption | null) => void;
}

// form for submitting files to the api
// all state is controlled from above, and necessary elements and control functions are passed in
export const FileHandlerStepOne = ({
    fileType,
    schemaOptions,
    selectedSchemaOption,
    onSchemaChange,
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
                <option value="">Select a schema</option>
                {schemaOptions.map(({ title, value }) => (
                    <option key={value} value={value}>
                        {title}
                    </option>
                ))}
            </Dropdown>
        </div>
    );
};
