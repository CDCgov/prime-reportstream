import React, { useEffect, useState } from "react";
import { GridContainer } from "@trussworks/react-uswds";

import Spinner from "../Spinner";
import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { USLink } from "../USLink";
import { showError } from "../AlertNotifications";
import { ApiKey } from "../../config/endpoints/settings";
import { useSessionContext } from "../../contexts/SessionContext";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { validateFileType, validateFileSize } from "../../utils/FileUtils";
import useCreateOrganizationPublicKey from "../../hooks/network/Organizations/PublicKeys/UseCreateOrganizationPublicKey";
import useOrganizationPublicKeys from "../../hooks/network/Organizations/PublicKeys/UseOrganizationPublicKeys";
import useOrganizationSenders from "../../hooks/UseOrganizationSenders";
import Alert from "../../shared/Alert/Alert";
import { FeatureName } from "../../AppRouter";
import { trackAppInsightEvent } from "../../utils/Analytics";

import ManagePublicKeyChooseSender from "./ManagePublicKeyChooseSender";
import ManagePublicKeyUpload from "./ManagePublicKeyUpload";
import ManagePublicKeyUploadSuccess from "./ManagePublicKeyUploadSuccess";
import ManagePublicKeyUploadError from "./ManagePublicKeyUploadError";
import ManagePublicKeyConfigured from "./ManagePublicKeyConfigured";

export const CONTENT_TYPE = "application/x-x509-ca-cert";
export const FORMAT = "PEM";

export function ManagePublicKey() {
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
        isLoading: isUploading,
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
            showError("No file contents to validate.");
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
            trackAppInsightEvent(featureEvent, {
                fileUpload: {
                    status: `Error: ${e.toString()}`,
                    fileName: file?.name,
                    fileType: file?.type,
                    fileSize: file?.size,
                    sender: sender,
                },
            });
            showError(`Uploading public key failed. ${e.toString()}`);
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
            showError(fileTypeError);
        }
        const fileSizeError = validateFileSize(file);
        if (fileSizeError) {
            showError(fileSizeError);
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

    const showPublicKeyConfigured =
        sender && hasPublicKey && !uploadNewPublicKey && !fileSubmitted;
    const showUploadMsg =
        (sender && !fileSubmitted && !hasPublicKey) ||
        (!uploadNewPublicKey && !sender);
    const isUploadEnabled =
        (sender && !fileSubmitted && !hasPublicKey) || uploadNewPublicKey;
    const hasUploadError = fileSubmitted && !isUploading && !isSuccess;

    if (isSuccess) {
        trackAppInsightEvent(featureEvent, {
            fileUpload: {
                status: "Success",
                fileName: file?.name,
                fileType: file?.type,
                fileSize: file?.size,
                sender: sender,
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
                            Learn more about
                            <USLink href="/resources/api/getting-started#set-up-authentication">
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

export const ManagePublicKeyWithAuth = () => (
    <AuthElement
        element={withCatchAndSuspense(<ManagePublicKey />)}
        requiredUserType={MemberType.SENDER}
    />
);
