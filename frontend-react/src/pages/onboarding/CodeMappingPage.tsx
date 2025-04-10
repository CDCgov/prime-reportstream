import type { PropsWithChildren } from "react";
import CodeMappingForm from "../../components/CodeMapping/CodeMappingForm";

export type CodeMappingPageProps = PropsWithChildren;

const CodeMappingPage = (props: CodeMappingPageProps) => {
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
                                                <a href="/developer-resources/api-onboarding-guide#3_4">instructions</a>{" "}
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
