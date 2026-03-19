import { Button, GridContainer } from "@trussworks/react-uswds";
import { AxiosError } from "axios";
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
import { Alert } from "../../shared";
import { parseCsvForError } from "../../utils/FileUtils";
import { getClientHeader } from "../../utils/SessionStorageTools";

enum CodeMappingSteps {
    StepFail = "CodeMapFail",
    StepOne = "CodeMapFileSelect",
    StepTwo = "CodeMapResult",
}

const CodeMappingPage = () => {
    const { activeMembership } = useSessionContext();
    const { data: senderDetail } = useOrganizationSender();
    const { mutate, isPending, data, client, setClient } = useCodeMappingFormSubmit();
    const [currentCodeMapStep, setCurrentCodeMapStep] = useState<CodeMappingSteps>(CodeMappingSteps.StepOne);
    const [file, setFile] = useState<string>("");
    const [fileName, setFileName] = useState("");
    const [apiError, setApiError] = useState<AxiosError | null>(null);
    const onCancelHandler = useCallback<MouseEventHandler>((_ev) => {
        // Don't have a proper mechanism to cancel in-flight requests so refresh page
        window.location.reload();
    }, []);
    const onReset = () => {
        setCurrentCodeMapStep(CodeMappingSteps.StepOne);
    };

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
        mutate(
            { file: file, client },
            {
                onSuccess: () => {
                    setCurrentCodeMapStep(CodeMappingSteps.StepTwo);
                },
                onError: (err) => {
                    setApiError(err.cause as AxiosError);
                    setCurrentCodeMapStep(CodeMappingSteps.StepFail);
                },
            },
        );
    };

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
                        {currentCodeMapStep === CodeMappingSteps.StepFail && (
                            <div>
                                {fileName && (
                                    <h2 className="margin-bottom-0">
                                        <span className="text-normal font-body-md text-base margin-bottom-0">
                                            <p>File Name</p>
                                            <p>{fileName}</p>
                                        </span>
                                    </h2>
                                )}
                                <Alert
                                    type={"error"}
                                    className="margin-bottom-10"
                                    heading={
                                        apiError?.response?.statusText && apiError?.response?.status
                                            ? `${apiError.response.statusText} ${apiError.response.status}`.trim()
                                            : "File could not be uploaded"
                                    }
                                >
                                    <>
                                        {apiError?.response?.data ? (
                                            <p>{JSON.stringify(apiError.response.data)}</p>
                                        ) : (
                                            <p>
                                                Check the{" "}
                                                <a href="/developer-resources/api/getting-started#2_4">instructions</a>{" "}
                                                and <a href={site.assets.codeMapTemplate.path}>template</a> to make sure
                                                you have the correct file type and that your file includes the columns{" "}
                                                <span className="text-bold">Code, Name,</span> and{" "}
                                                <span className="text-bold">Coding system</span>.
                                            </p>
                                        )}
                                    </>
                                </Alert>

                                <Button type="button" onClick={onReset}>
                                    Test another file
                                </Button>
                            </div>
                        )}
                        {currentCodeMapStep === CodeMappingSteps.StepOne && (
                            <CodeMappingForm
                                onSubmit={handleSubmit}
                                setFile={(e) => {
                                    void handleFileSelect(e);
                                }}
                            />
                        )}
                        {currentCodeMapStep === CodeMappingSteps.StepTwo && (
                            <CodeMappingResults data={data ?? []} fileName={fileName} onReset={onReset} />
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
