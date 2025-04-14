import { Button, ButtonGroup, FileInput } from "@trussworks/react-uswds";
import { ChangeEvent, FormEventHandler, MouseEventHandler, useCallback } from "react";
import site from "../../content/site.json";

interface CodeMappingFormProps {
    onSubmit: FormEventHandler<HTMLFormElement>;
    setFile: (event: ChangeEvent<HTMLInputElement>) => void;
}

const CodeMappingForm = ({ onSubmit, setFile }: CodeMappingFormProps) => {
    const onBackHandler = useCallback<MouseEventHandler>((_ev) => {
        window.history.back();
    }, []);

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

            <form onSubmit={onSubmit}>
                <label className="usa-label" htmlFor="file-input-specific">
                    Upload CSV file
                </label>
                <span className="usa-hint" id="file-input-specific-hint">
                    Make sure your file has a .csv extension
                </span>
                <FileInput id={""} name={"file"} className="maxw-full" accept=".csv" onChange={setFile} />
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
