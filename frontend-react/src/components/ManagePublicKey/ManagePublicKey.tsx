import React, { useState } from "react";
import { GridContainer, Icon, SiteAlert } from "@trussworks/react-uswds";

import Spinner from "../Spinner";
import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { USLink } from "../USLink";
import { showError } from "../AlertNotifications";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { validateFileType, validateFileSize } from "../../utils/FileUtils";
import { useCreateOrganizationPublicKey } from "../../hooks/UseCreateOrganizationPublicKey";

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
    const [showBack, setShowBack] = useState(false);
    const [fileContent, setFileContent] = useState("");
    const [file, setFile] = useState<File | null>(null);
    const [fileSubmitted, setFileSubmitted] = useState(false);

    const {
        mutateAsync,
        isSuccess,
        isLoading: isUploading,
    } = useCreateOrganizationPublicKey();

    const handleSenderSelect = (selectedSender: string, showBack: boolean) => {
        setSender(selectedSender);
        setShowBack(showBack);
    };

    const handlePublicKeySubmit = async (
        event: React.FormEvent<HTMLFormElement>
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
            showError(`Uploading public key failed. ${e.toString()}`);
        }
    };

    const handleFileChange = async (
        event: React.ChangeEvent<HTMLInputElement>
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

    const showPublicKeyConfigured =
        sender.length > 0 &&
        hasPublicKey &&
        !uploadNewPublicKey &&
        !fileSubmitted;
    const showUploadMsg =
        (sender.length > 0 && !fileSubmitted && !hasPublicKey) ||
        (!uploadNewPublicKey && sender.length === 0);
    const showUpload =
        (sender.length > 0 && !fileSubmitted && !hasPublicKey) ||
        uploadNewPublicKey;
    const showUploadError = fileSubmitted && !isUploading && !isSuccess;

    return (
        <GridContainer className="manage-public-key padding-bottom-5 tablet:padding-top-6">
            {!isUploading && (
                <h1 className="margin-top-0 margin-bottom-3">
                    Manage public key
                </h1>
            )}
            {showUploadMsg && (
                <>
                    <p className="font-sans-md">
                        Send your public key to begin the REST API
                        authentication process.
                    </p>
                    <SiteAlert variant="info" showIcon={false}>
                        <Icon.Lightbulb />
                        <span className="padding-left-1">
                            If you need more information on generating your
                            public key, reference page 7 in the{" "}
                            <USLink href="/resources/programmers-guide">
                                API Programmer’s Guide.
                            </USLink>
                        </span>
                    </SiteAlert>
                </>
            )}
            {sender.length === 0 && (
                <ManagePublicKeyChooseSender
                    onSenderSelect={handleSenderSelect}
                />
            )}
            {showPublicKeyConfigured && (
                <ManagePublicKeyConfigured
                    onUploadNewPublicKey={() => setUploadNewPublicKey(true)}
                />
            )}
            {uploadNewPublicKey && (
                <p className="font-sans-md">
                    Your public key is already configured.
                </p>
            )}
            {showUpload && (
                <ManagePublicKeyUpload
                    onPublicKeySubmit={handlePublicKeySubmit}
                    onFileChange={handleFileChange}
                    onBack={() => setSender("")}
                    onFetchPublicKey={setHasPublicKey}
                    showBack={showBack}
                    file={file}
                    sender={sender}
                />
            )}
            {isUploading && <Spinner />}
            {isSuccess && <ManagePublicKeyUploadSuccess />}
            {showUploadError && (
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
