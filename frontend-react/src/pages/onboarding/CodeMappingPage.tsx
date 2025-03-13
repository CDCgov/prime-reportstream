import { Button, GridContainer } from "@trussworks/react-uswds";
import { ChangeEvent, FormEvent, MouseEventHandler, useCallback, useState } from "react";
import { Helmet } from "react-helmet-async";
import CodeMappingForm from "../../components/CodeMapping/CodeMappingForm";
import CodeMappingResults from "../../components/CodeMapping/CodeMappingResults";
import Spinner from "../../components/Spinner";
import { USExtLink } from "../../components/USLink";
import site from "../../content/site.json";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { showToast } from "../../contexts/Toast";
import useOrganizationSender from "../../hooks/api/organizations/UseOrganizationSender/UseOrganizationSender";
import useCodeMappingFormSubmit from "../../hooks/api/UseCodeMappingFormSubmit/UseCodeMappingFormSubmit";
import { parseCsvForError } from "../../utils/FileUtils";
import { getClientHeader } from "../../utils/SessionStorageTools";

enum CodeMappingSteps {
    StepOne = "CodeMapFileSelect",
    StepTwo = "CodeMapResult",
}

const CodeMappingPage = () => {
    const { activeMembership } = useSessionContext();
    const { data: senderDetail } = useOrganizationSender();
    const { data, isLoading, setRequestBody, setClient } = useCodeMappingFormSubmit();
    const [currentCodeMapStep, setCurrentCodeMapStep] = useState<CodeMappingSteps>(CodeMappingSteps.StepOne);
    const [file, setFile] = useState<File | null>(null);
    const [fileName, setFileName] = useState("");
    const onCancelHandler = useCallback<MouseEventHandler>((_ev) => {
        // Don't have a proper mechanism to cancel in-flight requests so refresh page
        window.location.reload();
    }, []);
    const onReset = () => {
        setCurrentCodeMapStep(CodeMappingSteps.StepOne);
    };

    console.log("senderDetail = ", senderDetail);

    const handleFileSelect = async (event: ChangeEvent<HTMLInputElement>) => {
        event.preventDefault();
        if (!event?.target?.files?.length) {
            return;
        }
        const selectedFile = event.target.files.item(0)!;

        const selectedFileContent = await selectedFile.text();

        if (selectedFile.type === "csv" || selectedFile.type === "text/csv") {
            const localCsvError = parseCsvForError(selectedFile.name, selectedFileContent);
            if (localCsvError) {
                showToast(localCsvError, "error");
                return;
            }
        }
        setClient(getClientHeader(senderDetail?.schemaName, activeMembership, senderDetail ?? undefined));
        setFileName(selectedFile.name);
        setFile(selectedFileContent);
    };

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setRequestBody(file);
        setCurrentCodeMapStep(CodeMappingSteps.StepTwo);
    };

    return (
        <>
            <Helmet>
                <title>Code mapping tool - ReportStream</title>
            </Helmet>

            <GridContainer>
                <h1>Code mapping tool</h1>
                {isLoading ? (
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
                            <CodeMappingForm
                                onSubmit={handleSubmit}
                                setFile={(e) => {
                                    void handleFileSelect(e);
                                }}
                            />
                        )}
                        {currentCodeMapStep === CodeMappingSteps.StepTwo && (
                            <CodeMappingResults data={data} fileName={fileName} onReset={onReset} />
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
