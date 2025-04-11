import { Button, ButtonGroup, FileInput } from "@trussworks/react-uswds";
import { FormEventHandler, MouseEventHandler, type PropsWithChildren, useCallback } from "react";
import CodeMappingResults from "./CodeMappingResults";
import site from "../../content/site.json";
import useCodeMappingFormSubmit from "../../hooks/api/UseCodeMappingFormSubmit/UseCodeMappingFormSubmit";
import Spinner from "../Spinner";
import { USLink } from "../USLink";

export type CodeMappingFormProps = PropsWithChildren;

const CodeMappingForm = (props: CodeMappingFormProps) => {
    const { data, isPending, mutate } = useCodeMappingFormSubmit();
    /**
     * TODO: Implement submit handler
     */
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
    const onCancelHandler = useCallback<MouseEventHandler>((_ev) => {
        // Don't have a proper mechanism to cancel in-flight requests so refresh page
        window.location.reload();
    }, []);

    return (
        <>
            <p className="font-sans-lg">
                Check that your LOINC and SNOMED codes are mapped to conditions so ReportStream can accurately transform
                your data
            </p>
            <p>
                Follow <a href={`${site.developerResources.apiOnboardingGuide.url}#3_4`}>these instructions</a> and use{" "}
                <a href={site.assets.codeMapTemplate.path}>our template</a> to format your result and organism codes to
                LOINC or SNOMED. Note: Local codes cannot be automatically mapped.
            </p>
            {data && <CodeMappingResults />}
            {isPending && (
                <>
                    <Spinner />
                    <p className="text-center">
                        Checking your file for any unmapped codes that will <br /> prevent data from being reported
                        successfully
                    </p>
                    <Button type={"button"} outline onClick={onCancelHandler}>
                        Cancel
                    </Button>
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
                    <FileInput id={""} name={"file"} className="maxw-full" accept=".csv" />
                    <ButtonGroup className="margin-top-5">
                        <Button type={"button"} outline onClick={onBackHandler}>
                            Back
                        </Button>
                        <Button type={"submit"}>Submit</Button>
                    </ButtonGroup>
                </form>
            )}
            {props.children}
            <p className="margin-top-9">
                Questions or feedback? Please email{" "}
                <USLink href="mailto:reportstream@cdc.gov">reportstream@cdc.gov</USLink>
            </p>
        </>
    );
};

export default CodeMappingForm;
