import { Button, ButtonGroup, FileInput } from "@trussworks/react-uswds";
import { FormEventHandler, MouseEventHandler, useCallback } from "react";
import site from "../../content/site.json";

interface CodeMappingFormProps {
    onSubmitHandler: FormEventHandler<HTMLFormElement>;
    setFileName: (fileName: string) => void;
}

const CodeMappingForm = ({ onSubmitHandler, setFileName }: CodeMappingFormProps) => {
    const onBackHandler = useCallback<MouseEventHandler>((_ev) => {
        window.history.back();
    }, []);

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const files = event.target.files;
        if (!files || files.length === 0) return;

        // Take the first file name
        setFileName(files[0].name);
    };

    return (
        <>
            <p className="font-sans-lg">
                Check that your LOINC and SNOMED codes are mapped to conditions so ReportStream can accurately transform
                your data
            </p>
            <p>
                Follow <a href="/developer-resources/api/getting-started#2_4">these instructions</a> and use{" "}
                <a href={site.assets.codeMapTemplate.path}>our template</a> to format your result and organism codes to
                LOINC or SNOMED. Note: Local codes cannot be automatically mapped.
            </p>

            <form onSubmit={onSubmitHandler}>
                <label className="usa-label" htmlFor="file-input-specific">
                    Upload CSV file
                </label>
                <span className="usa-hint" id="file-input-specific-hint">
                    Make sure your file has a .csv extension
                </span>
                <FileInput id={""} name={"file"} className="maxw-full" accept=".csv" onChange={handleFileChange} />
                <ButtonGroup className="margin-top-5">
                    <Button type={"button"} outline onClick={onBackHandler}>
                        Back
                    </Button>
                    <Button type={"submit"}>Submit</Button>
                </ButtonGroup>
            </form>
        </>
    );
};

export default CodeMappingForm;
