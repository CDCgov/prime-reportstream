import { GridContainer } from "@trussworks/react-uswds";
import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import { Helmet } from "react-helmet-async";

import ManagePublicKeyChooseSender from "./ManagePublicKeyChooseSender";
import ManagePublicKeyConfigured from "./ManagePublicKeyConfigured";
import ManagePublicKeyUpload from "./ManagePublicKeyUpload";
import ManagePublicKeyUploadError from "./ManagePublicKeyUploadError";
import ManagePublicKeyUploadSuccess from "./ManagePublicKeyUploadSuccess";
import { ApiKey } from "../../config/endpoints/settings";
import site from "../../content/site.json";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { showToast } from "../../contexts/Toast";
import useCreateOrganizationPublicKey from "../../hooks/api/organizations/UseCreateOrganizationPublicKey/UseCreateOrganizationPublicKey";
import useOrganizationPublicKeys from "../../hooks/api/organizations/UseOrganizationPublicKeys/UseOrganizationPublicKeys";
import useOrganizationSenders from "../../hooks/api/organizations/UseOrganizationSenders/UseOrganizationSenders";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import Alert from "../../shared/Alert/Alert";
import { FeatureName } from "../../utils/FeatureName";
import { validateFileSize, validateFileType } from "../../utils/FileUtils";
import Spinner from "../Spinner";
import { USLink } from "../USLink";

export const CONTENT_TYPE = "application/x-x509-ca-cert";
export const FORMAT = "PEM";

export function ManagePublicKeyPage() {
    const appInsights = useAppInsightsContext();
    const [hasPublicKey, setHasPublicKey] = useState(false);
    const [uploadNewPublicKey, setUploadNewPublicKey] = useState(false);
    const [sender, setSender] = useState("");
    const [hasBack, setHasBack] = useState(false);
    const [fileContent, setFileContent] = useState("");
    const [file, setFile] = useState<File | null>(null);
    const [fileSubmitted, setFileSubmitted] = useState(false);

    const { activeMembership } = useSessionContext();
    const { data: senders } = useOrganizationSenders();
    const { data: orgPublicKeys } = useOrganizationPublicKeys();
    const { mutateAsync, isSuccess, isPending: isUploading } = useCreateOrganizationPublicKey();

    const featureEvent = `${FeatureName.PUBLIC_KEY}`;

    const handleSenderSelect = (selectedSender: string, showBack: boolean) => {
        setSender(selectedSender);
        setHasBack(showBack);
    };

    const handleOnBack = () => {
        setSender("");
        setUploadNewPublicKey(false);
    };

    const handlePublicKeySubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (fileContent.length === 0) {
            showToast("No file contents to validate.", "error");
            return;
        }

        try {
            setFileSubmitted(true);
            setUploadNewPublicKey(false);

            await mutateAsync({
                kid: fileContent,
                sender: sender,
            });
        } catch (e: any) {
            appInsights?.trackEvent({
                name: featureEvent,
                properties: {
                    fileUpload: {
                        status: `Error: ${e.toString()}`,
                        fileName: file?.name,
                        fileType: file?.type,
                        fileSize: file?.size,
                        sender: sender,
                    },
                },
            });
            showToast(`Uploading public key failed. ${e.toString()}`, "error");
        }
    };

    const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        // No file selected
        if (!event?.target?.files?.length) {
            setFile(null);
            return;
        }

        const file = event.target.files.item(0);
        if (!file) return; // so typescript doesnt complain

        const content = await file.text();

        const fileTypeError = validateFileType(file, FORMAT, CONTENT_TYPE);
        if (fileTypeError) {
            showToast(fileTypeError, "error");
        }
        const fileSizeError = validateFileSize(file);
        if (fileSizeError) {
            showToast(fileSizeError, "error");
        }

        if (!fileTypeError && !fileSizeError) {
            setFile(file);
            setFileContent(content);
        }
    };

    useEffect(() => {
        if (sender && orgPublicKeys?.keys.length) {
            // check if kid already exists for the selected org.sender
            const kid = `${activeMembership?.parsedName}.${sender}`;
            for (const apiKeys of orgPublicKeys.keys) {
                if (apiKeys.keys.some((k: ApiKey) => k.kid === kid)) {
                    setHasPublicKey(true);
                }
            }
        }

        if (senders?.length === 1) {
            setSender(senders[0].name);
            setHasBack(false);
        }
    }, [orgPublicKeys, sender, activeMembership?.parsedName, senders]);

    const showPublicKeyConfigured = sender && hasPublicKey && !uploadNewPublicKey && !fileSubmitted;
    const showUploadMsg = (sender && !fileSubmitted && !hasPublicKey) || (!uploadNewPublicKey && !sender);
    const isUploadEnabled = (sender && !fileSubmitted && !hasPublicKey) || uploadNewPublicKey;
    const hasUploadError = fileSubmitted && !isUploading && !isSuccess;

    if (isSuccess) {
        appInsights?.trackEvent({
            name: featureEvent,
            properties: {
                fileUpload: {
                    status: "Success",
                    fileName: file?.name,
                    fileType: file?.type,
                    fileSize: file?.size,
                    sender: sender,
                },
            },
        });
    }

    return (
        <>
            <Helmet>
                <title>ReportStream API - Manage public key</title>
                <meta name="description" content="Send your public key to begin the REST API authentication process." />
                <meta property="og:image" content="/assets/img/opengraph/howwehelpyou-3.png" />
                <meta property="og:image:alt" content="An abstract illustration of screens and a document." />
            </Helmet>
            <GridContainer className="manage-public-key padding-bottom-5 tablet:padding-top-6">
                {!isUploading && <h1 className="margin-top-0 margin-bottom-5">Manage public key</h1>}
                {showUploadMsg && (
                    <>
                        <p className="font-sans-md">
                            Send your public key to begin the REST API authentication process.
                        </p>
                        <Alert type="tip" className="margin-bottom-6">
                            <span className="padding-left-1">
                                Learn more about{" "}
                                <USLink
                                    href={`${site.developerResources.apiOnboardingGuide.url}#set-up-authentication-and-test-your-api-connection`}
                                >
                                    generating your public key
                                </USLink>{" "}
                                and setting up authentication.
                            </span>
                        </Alert>
                    </>
                )}
                {!sender && <ManagePublicKeyChooseSender senders={senders ?? []} onSenderSelect={handleSenderSelect} />}
                {showPublicKeyConfigured && <ManagePublicKeyConfigured />}
                {isUploadEnabled && (
                    <ManagePublicKeyUpload
                        onPublicKeySubmit={(ev) => void handlePublicKeySubmit(ev)}
                        onFileChange={(ev) => void handleFileChange(ev)}
                        onBack={handleOnBack}
                        hasBack={hasBack}
                        publicKey={hasPublicKey ?? file}
                        file={file}
                    />
                )}
                {isUploading && <Spinner />}
                {isSuccess && <ManagePublicKeyUploadSuccess />}
                {hasUploadError && <ManagePublicKeyUploadError onTryAgain={() => setFileSubmitted(false)} />}
            </GridContainer>
        </>
    );
}

export default ManagePublicKeyPage;
