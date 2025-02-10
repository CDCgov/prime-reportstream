import { Button, FileInput } from "@trussworks/react-uswds";
import { FormEventHandler, MouseEventHandler, type PropsWithChildren, useCallback } from "react";
import CodeMappingResults from "./CodeMappingResults";
import useCodeMappingFormSubmit from "../../hooks/api/UseCodeMappingFormSubmit/UseCodeMappingFormSubmit";
import Spinner from "../Spinner";

export type CodeMappingFormProps = PropsWithChildren;

const CodeMappingForm = (props: CodeMappingFormProps) => {
    const { data, isPending, mutate } = useCodeMappingFormSubmit();
    const onSubmitHandler = useCallback<FormEventHandler<HTMLFormElement>>(
        (ev) => {
            ev.preventDefault();
            mutate();
            return false;
        },
        [mutate],
    );
    const onBackHandler = useCallback<MouseEventHandler>((_ev) => {
        window.history.back();
    }, []);

    return (
        <>
            <p className="todo">
                Check that your LOINC and SNOMED codes are mapped to conditions so ReportStream can accurately transform
                your data
            </p>
            <p>
                Follow <a href="todo">these instructions</a> and use <a href="todo">our template</a> to format your
                result and organism codes to LOINC or SNOMED. Note: Local codes cannot be automatically mapped.
            </p>
            {data && <CodeMappingResults />}
            {isPending && (
                <>
                    <Spinner />
                    <p>
                        Checking your file for any unmapped codes that will prevent data from being reported
                        successfully.
                    </p>
                </>
            )}
            {!data && !isPending && (
                <form onSubmit={onSubmitHandler}>
                    <label className="usa-label" htmlFor="file-input-specific">
                        Upload CSV file
                    </label>
                    <span className="usa-hint" id="file-input-specific-hint">
                        Make sure your file has a .csv extension
                    </span>
                    <FileInput id={""} name={"file"} />
                    <Button type={"button"} outline onClick={onBackHandler}>
                        Back
                    </Button>
                    <Button type={"submit"}>Submit</Button>
                </form>
            )}
            {props.children}
            <p>
                Questions or feedback? Please email <a href="todo">reportstream@cdc.gov</a>
            </p>
        </>
    );
};

export default CodeMappingForm;
