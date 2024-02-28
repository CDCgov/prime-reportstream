import {
    ErrorMessage,
    FileInput,
    FileInputRef,
    FormGroup,
    Label,
} from "@trussworks/react-uswds";
import React, {
    useState,
    useRef,
    ChangeEvent,
    DragEventHandler,
    ReactElement,
} from "react";

export default {
    title: "Components/File input",
    component: FileInput,
    argTypes: {
        onChange: { action: "changed" },
        onDrop: { action: "dropped" },
    },
};

type StorybookArguments = {
    onChange: (event: ChangeEvent<Element>) => void;
    onDrop: DragEventHandler<Element>;
};

export const singleFileInput = (): ReactElement => (
    <FormGroup>
        <Label htmlFor="file-input-single">Input accepts a single file</Label>
        <FileInput id="file-input-single" name="file-input-single" />
    </FormGroup>
);

export const acceptTextAndPDF = (): ReactElement => (
    <FormGroup>
        <Label htmlFor="file-input-specific">
            Input accepts only specific file types
        </Label>
        <span className="usa-hint" id="file-input-specific-hint">
            Select PDF or TXT files
        </span>
        <FileInput
            id="file-input-specific"
            name="file-input-specific"
            accept=".pdf,.txt"
            aria-describedby="file-input-specific-hint"
            multiple
        />
    </FormGroup>
);

export const acceptImages = (): ReactElement => (
    <FormGroup>
        <Label htmlFor="file-input-wildcard">
            Input accepts any kind of image
        </Label>
        <span className="usa-hint" id="file-input-wildcard-hint">
            Select any type of image format
        </span>
        <FileInput
            id="file-input-wildcard"
            name="file-input-wildcard"
            accept="image/*"
            aria-describedby="file-input-wildcard-hint"
            multiple
        />
    </FormGroup>
);

export const multipleFilesInput = (): ReactElement => (
    <FormGroup>
        <Label htmlFor="file-input-multiple">
            Input accepts multiple files
        </Label>
        <span className="usa-hint" id="file-input-multiple-hint">
            Select one or more files
        </span>
        <FileInput
            id="file-input-multiple"
            name="file-input-multiple"
            aria-describedby="file-input-multiple-hint"
            multiple
        />
    </FormGroup>
);

export const withError = (): ReactElement => (
    <div style={{ marginLeft: "1.25em" }}>
        <FormGroup error>
            <Label htmlFor="file-input-multiple" error>
                Input has an error
            </Label>
            <span className="usa-hint" id="file-input-error-hint">
                Select any valid file
            </span>
            <ErrorMessage id="file-input-error-alert">
                Display a helpful error message
            </ErrorMessage>
            <FileInput
                id="file-input-error"
                name="file-input-error"
                aria-describedby="file-input-error-hint file-input-error-alert"
            />
        </FormGroup>
    </div>
);

export const disabled = (): ReactElement => (
    <FormGroup>
        <Label htmlFor="file-input-disabled">Input in a disabled state</Label>
        <FileInput
            id="file-input-disabled"
            name="file-input-disabled"
            disabled
        />
    </FormGroup>
);

export const withRefAndCustomHandlers = (
    argTypes: StorybookArguments,
): ReactElement => {
    const [files, setFiles] = useState<FileList | null>(null);
    const fileInputRef = useRef<FileInputRef>(null);

    const handleClearFiles = (): void => fileInputRef.current?.clearFiles();

    const handleChange = (e: ChangeEvent<HTMLInputElement>): void => {
        argTypes.onChange(e);
        setFiles(e.target?.files);
    };

    const fileList = [];
    if (files) {
        for (let i = 0; i < files?.length; i++) {
            fileList.push(<li key={`file_${i}`}>{files?.[Number(i)].name}</li>);
        }
    }

    return (
        <>
            <FormGroup>
                <Label htmlFor="file-input-async">
                    Input implements custom handlers
                </Label>
                <FileInput
                    id="file-input-async"
                    name="file-input-async"
                    multiple
                    onChange={handleChange}
                    onDrop={argTypes.onDrop}
                    ref={fileInputRef}
                />
            </FormGroup>

            <button type="button" onClick={handleClearFiles}>
                Clear files
            </button>

            <p>{files?.length || 0} files added:</p>
            <ul>{fileList}</ul>
        </>
    );
};

export const customText = (): ReactElement => (
    <FormGroup>
        <Label htmlFor="file-input-single">
            La entrada acepta un solo archivo
        </Label>
        <FileInput
            id="file-input-single"
            name="file-input-single"
            dragText="Arrastre el archivo aquí o "
            chooseText="elija de una carpeta"
        />
    </FormGroup>
);
