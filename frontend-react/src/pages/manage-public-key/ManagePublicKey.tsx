import React, { useCallback, useEffect, useState } from "react";
import { GridContainer } from "@trussworks/react-uswds";

import Spinner from "../../components/Spinner";
import { USLink } from "../../components/USLink";
import { useToast } from "../../contexts/Toast";
import { ApiKey, ApiKeySet, RSSender } from "../../config/endpoints/settings";
import { useSessionContext } from "../../contexts/Session";
import { validateFileType, validateFileSize } from "../../utils/FileUtils";
import useCreateOrganizationPublicKey, {
    OrganizationPublicKeyPostArgs,
} from "../../hooks/network/Organizations/PublicKeys/UseCreateOrganizationPublicKey";
import useOrganizationPublicKeys from "../../hooks/network/Organizations/PublicKeys/UseOrganizationPublicKeys";
import useOrganizationSenders from "../../hooks/UseOrganizationSenders";
import Alert from "../../shared/Alert/Alert";
import { FeatureName } from "../../utils/FeatureName";
import { useAppInsightsContext } from "../../contexts/AppInsights";
import ManagePublicKeyChooseSender from "../../components/ManagePublicKey/ManagePublicKeyChooseSender";
import ManagePublicKeyUpload from "../../components/ManagePublicKey/ManagePublicKeyUpload";
import ManagePublicKeyUploadSuccess from "../../components/ManagePublicKey/ManagePublicKeyUploadSuccess";
import ManagePublicKeyUploadError from "../../components/ManagePublicKey/ManagePublicKeyUploadError";
import ManagePublicKeyConfigured from "../../components/ManagePublicKey/ManagePublicKeyConfigured";

export const CONTENT_TYPE = "application/x-x509-ca-cert";
export const FORMAT = "PEM";

export interface ManagePublicKeyPageBaseProps {
    organization: string;
    onSubmitPublicKey: (
        key: OrganizationPublicKeyPostArgs,
        file: File,
    ) => Promise<void>;
    onToast: (...args: any[]) => void;
    keySets?: ApiKeySet[];
    senders?: RSSender[];
    isSubmitting?: boolean;
    isLoading?: boolean;
    isSuccess?: boolean;
}

export function ManagePublicKeyPageBase({
    onSubmitPublicKey,
    keySets = [],
    senders = [],
    isLoading = false,
    isSubmitting = false,
    isSuccess = false,
    organization,
    onToast,
}: ManagePublicKeyPageBaseProps) {
    const [hasPublicKey, setHasPublicKey] = useState(false);
    const [uploadNewPublicKey, setUploadNewPublicKey] = useState(false);
    const [sender, setSender] = useState("");
    const [hasBack, setHasBack] = useState(false);
    const [fileContent, setFileContent] = useState("");
    const [file, setFile] = useState<File | null>(null);
    const [fileSubmitted, setFileSubmitted] = useState(false);

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

        try {
            if (!file || !fileContent || !sender)
                throw new Error("File, or sender missing");

            await onSubmitPublicKey(
                {
                    kid: fileContent,
                    sender,
                },
                file,
            );
        } catch (e: any) {
            onToast(
                new Error(`Uploading public key failed. ${e.toString()}`, {
                    cause: e,
                }),
                "error",
            );
        }
    };

    const handleFileChange = async (
        event: React.ChangeEvent<HTMLInputElement>,
    ) => {
        try {
            const file = event.target.files?.[0];
            // No file selected
            if (!file) {
                setFile(null);
                return;
            }

            const content = await file.text();

            if (!content) {
                throw new Error("No file contents to validate.");
            }

            validateFileType(file, FORMAT, CONTENT_TYPE);
            validateFileSize(file);
            setFile(file);
            setFileContent(content);
        } catch (e: any) {
            onToast(e, "error");
        }
    };

    useEffect(() => {
        if (sender && keySets.length) {
            // check if kid already exists for the selected org.sender
            const kid = `${organization}.${sender}`;
            for (const apiKeys of keySets) {
                if (apiKeys.keys.some((k: ApiKey) => k.kid === kid)) {
                    setHasPublicKey(true);
                }
            }
        }

        if (senders?.length === 1) {
            setSender(senders[0].name);
            setHasBack(false);
        }
    }, [keySets, organization, sender, senders]);

    const showPublicKeyConfigured =
        sender && hasPublicKey && !uploadNewPublicKey && !fileSubmitted;
    const showUploadMsg =
        (sender && !fileSubmitted && !hasPublicKey) ||
        (!uploadNewPublicKey && !sender);
    const isUploadEnabled =
        (sender && !fileSubmitted && !hasPublicKey) || uploadNewPublicKey;
    const hasUploadError = fileSubmitted && !isSubmitting && !isSuccess;

    return (
        <GridContainer className="manage-public-key padding-bottom-5 tablet:padding-top-6">
            {!isSubmitting && (
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
            {(isSubmitting || isLoading) && <Spinner />}
            {isSuccess && <ManagePublicKeyUploadSuccess />}
            {hasUploadError && (
                <ManagePublicKeyUploadError
                    onTryAgain={() => setFileSubmitted(false)}
                />
            )}
        </GridContainer>
    );
}

export function ManagePublicKeyPage() {
    const { appInsights } = useAppInsightsContext();
    const { data: senders, isLoading } = useOrganizationSenders();
    const { activeMembership: { parsedName } = { parsedName: "" } } =
        useSessionContext();
    const { data: { keys } = {} } = useOrganizationPublicKeys();
    const {
        mutateAsync: submitKey,
        isPending: isSubmitting,
        isSuccess,
    } = useCreateOrganizationPublicKey();
    const { toast } = useToast();

    const submitPublicKeyHandler = useCallback(
        async (key: OrganizationPublicKeyPostArgs, file: File) => {
            try {
                await submitKey(key);
                appInsights?.trackEvent({
                    name: FeatureName.PUBLIC_KEY,
                    properties: {
                        fileUpload: {
                            status: "Success",
                            fileName: file?.name,
                            fileType: file?.type,
                            fileSize: file?.size,
                            sender: key.sender,
                        },
                    },
                });
            } catch (e: any) {
                appInsights?.trackEvent({
                    name: FeatureName.PUBLIC_KEY,
                    properties: {
                        fileUpload: {
                            status: `Error: ${e.toString()}`,
                            fileName: file?.name,
                            fileType: file?.type,
                            fileSize: file?.size,
                            sender: key.sender,
                        },
                    },
                });
                throw e;
            }
        },
        [appInsights, submitKey],
    );

    return (
        <ManagePublicKeyPageBase
            onSubmitPublicKey={submitPublicKeyHandler}
            organization={parsedName}
            keySets={keys}
            isLoading={isLoading}
            isSubmitting={isSubmitting}
            senders={senders}
            onToast={toast}
            isSuccess={isSuccess}
        />
    );
}

export default ManagePublicKeyPage;
