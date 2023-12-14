import React, { useEffect, useState } from "react";
import { GridContainer } from "@trussworks/react-uswds";

import Spinner from "../Spinner";
import { USLink } from "../USLink";
import { showToast } from "../../contexts/Toast";
import { useSessionContext } from "../../contexts/Session";
import { validateFileType, validateFileSize } from "../../utils/FileUtils";
import useCreateOrganizationPublicKey from "../../hooks/network/Organizations/PublicKeys/UseCreateOrganizationPublicKey";
import useOrganizationPublicKeys from "../../hooks/network/Organizations/PublicKeys/UseOrganizationPublicKeys";
import useOrganizationSenders from "../../hooks/UseOrganizationSenders";
import Alert from "../../shared/Alert/Alert";
import { FeatureName } from "../../utils/FeatureName";
import { useAppInsightsContext } from "../../contexts/AppInsights";

import ManagePublicKeyChooseSender from "./ManagePublicKeyChooseSender";
import ManagePublicKeyUpload from "./ManagePublicKeyUpload";
import ManagePublicKeyUploadSuccess from "./ManagePublicKeyUploadSuccess";
import ManagePublicKeyUploadError from "./ManagePublicKeyUploadError";
import ManagePublicKeyConfigured from "./ManagePublicKeyConfigured";

export const CONTENT_TYPE = "application/x-x509-ca-cert";
export const FORMAT = "PEM";

export function ManagePublicKeyPage() {
    const { appInsights } = useAppInsightsContext();
    const [hasPublicKey, setHasPublicKey] = useState(false);
    const [uploadNewPublicKey, setUploadNewPublicKey] = useState(false);
    const [sender, setSender] = useState("");
    const [hasBack, setHasBack] = useState(false);
    const [fileContent, setFileContent] = useState("");
    const [file, setFile] = useState<File | null>(null);
    const [fileSubmitted, setFileSubmitted] = useState(false);

    const { activeMembership } = useSessionContext();
    const { data: senders, isLoading: isSendersLoading } =
        useOrganizationSenders();
    const { data: orgPublicKeys } = useOrganizationPublicKeys();
    const {
        mutateAsync,
        isSuccess,
        isPending: isUploading,
    } = useCreateOrganizationPublicKey();

    const featureEvent = `${FeatureName.PUBLIC_KEY}`;

    const handleSenderSelect = (selectedSender: string, showBack: boolean) => {
        setSender(selectedSender);
        setHasBack(showBack);
    };

    const handleOnBack = () => {
        setSender("");
        setUploadNewPublicKey(false);
    };

    const handlePublicKeySubmit = async (
        event: React.FormEvent<HTMLFormElement>,
    ) => {
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

    const handleFileChange = async (
        event: React.ChangeEvent<HTMLInputElement>,
    ) => {
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
                if (apiKeys.keys.some((k: RsJsonWebKey) => k.kid === kid)) {
                    setHasPublicKey(true);
                }
            }
        }

        if (senders?.length === 1) {
            setSender(senders[0].name);
            setHasBack(false);
        }
    }, [orgPublicKeys, sender, activeMembership?.parsedName, senders]);

    const showPublicKeyConfigured =
        sender && hasPublicKey && !uploadNewPublicKey && !fileSubmitted;
    const showUploadMsg =
        (sender && !fileSubmitted && !hasPublicKey) ||
        (!uploadNewPublicKey && !sender);
    const isUploadEnabled =
        (sender && !fileSubmitted && !hasPublicKey) || uploadNewPublicKey;
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
        <GridContainer className="manage-public-key padding-bottom-5 tablet:padding-top-6">
            {!isUploading && (
                <h1 className="margin-top-0 margin-bottom-5">
                    Manage public key
                </h1>
            )}
            {showUploadMsg && (
                <>
                    <p className="font-sans-md">
                        Send your public key to begin the REST API
                        authentication process.
                    </p>
                    <Alert type="tip" className="margin-bottom-6">
                        <span className="padding-left-1">
                            Learn more about{" "}
                            <USLink href="/developer-resources/api/getting-started#set-up-authentication">
                                generating your public key
                            </USLink>{" "}
                            and setting up authentication.
                        </span>
                    </Alert>
                </>
            )}
            {!sender && (
                <ManagePublicKeyChooseSender
                    senders={senders || []}
                    onSenderSelect={handleSenderSelect}
                />
            )}
            {showPublicKeyConfigured && <ManagePublicKeyConfigured />}
            {isUploadEnabled && (
                <ManagePublicKeyUpload
                    onPublicKeySubmit={handlePublicKeySubmit}
                    onFileChange={handleFileChange}
                    onBack={handleOnBack}
                    hasBack={hasBack}
                    publicKey={hasPublicKey ?? file}
                    file={file}
                />
            )}
            {(isUploading || isSendersLoading) && <Spinner />}
            {isSuccess && <ManagePublicKeyUploadSuccess />}
            {hasUploadError && (
                <ManagePublicKeyUploadError
                    onTryAgain={() => setFileSubmitted(false)}
                />
            )}
        </GridContainer>
    );
}

export default ManagePublicKeyPage;
