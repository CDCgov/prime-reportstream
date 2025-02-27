import { Button, GridContainer } from "@trussworks/react-uswds";
import { FormEventHandler, MouseEventHandler, useCallback, useState } from "react";
import { Helmet } from "react-helmet-async";
import CodeMappingForm from "../../components/CodeMapping/CodeMappingForm";
import CodeMappingResults from "../../components/CodeMapping/CodeMappingResults";
import Spinner from "../../components/Spinner";
import { USExtLink } from "../../components/USLink";
import site from "../../content/site.json";
import useCodeMappingFormSubmit, { sampArr } from "../../hooks/api/UseCodeMappingFormSubmit/UseCodeMappingFormSubmit";

enum CodeMappingSteps {
    StepOne = "CodeMapFileSelect",
    StepTwo = "CodeMapResult",
}

const CodeMappingPage = () => {
    const { data, isPending, mutate } = useCodeMappingFormSubmit();
    const [currentCodeMapStep, setCurrentCodeMapStep] = useState<CodeMappingSteps>(CodeMappingSteps.StepOne);
    const [fileName, setFileName] = useState("");
    const onCancelHandler = useCallback<MouseEventHandler>((_ev) => {
        // Don't have a proper mechanism to cancel in-flight requests so refresh page
        window.location.reload();
    }, []);
    const onReset = () => {
        setCurrentCodeMapStep(CodeMappingSteps.StepOne);
    };
    const onSubmit = useCallback<FormEventHandler<HTMLFormElement>>(
        (ev) => {
            ev.preventDefault();
            mutate();
            setCurrentCodeMapStep(CodeMappingSteps.StepTwo);
            return false;
        },
        [mutate],
    );
    return (
        <>
            <Helmet>
                <title>Code mapping tool - ReportStream</title>
            </Helmet>

            <GridContainer>
                <h1>Code mapping tool</h1>
                {isPending ? (
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
                ) : (
                    <>
                        {currentCodeMapStep === CodeMappingSteps.StepOne && (
                            <CodeMappingForm onSubmit={onSubmit} setFileName={setFileName} />
                        )}
                        {currentCodeMapStep === CodeMappingSteps.StepTwo && (
                            <CodeMappingResults fileName={fileName} data={data ?? sampArr} onReset={onReset} />
                        )}
                    </>
                )}

                <p className="margin-top-9">
                    Questions or feedback? Please email{" "}
                    <USExtLink href={`mailto: ${site.orgs.RS.email}`}>{site.orgs.RS.email}</USExtLink>{" "}
                </p>
            </GridContainer>
        </>
    );
};

export default CodeMappingPage;
