import React, { useCallback, useState } from "react";
import { GridContainer } from "@trussworks/react-uswds";
import { ErrorBoundary } from "react-error-boundary";

import Spinner from "../../components/Spinner";
import { USLink } from "../../components/USLink";
import { useToast } from "../../contexts/Toast";
import { ApiKeySet, RSSender } from "../../config/endpoints/settings";
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
    onSubmit: (key: OrganizationPublicKeyPostArgs, file: File) => Promise<void>;
    onError: (e: any) => void;
    keySets?: ApiKeySet[];
    senders?: RSSender[];
}

/**
 * Public key wizard with props for submission and error. Relies on the caller
 * to update keySets to include new key once created to determine submission
 * success.
 */
export function ManagePublicKeyPageBase({
    onSubmit,
    keySets = [],
    senders = [],
    onError,
}: ManagePublicKeyPageBaseProps) {
    const [sender, setSender] = useState<RSSender | undefined>(
        senders.length === 1 ? senders[0] : undefined,
    );
    const [file, setFile] = useState<File | null>(null);
    // Three states: undefined (not submitting), null (submitting), object (submitted args)
    const [submittedArgs, setSubmittedArgs] = useState<
        OrganizationPublicKeyPostArgs | undefined | null
    >();
    const publicKey = sender
        ? keySets?.some((s) =>
              s.keys.some(
                  (k) => k.kid === `${sender.organizationName}.${sender.name}`,
              ),
          )
        : undefined;
    const isSubmitting = submittedArgs === null;
    const isPublicKeySubmitted = publicKey && submittedArgs;
    const isPublicKeyFound = publicKey && !isPublicKeySubmitted;
    const isUploadEnabled = !publicKey && sender;
    const isUploadMessageVisible = isUploadEnabled && !isSubmitting;

    const handleSenderSelect = (selectedSender: string) => {
        setSender(senders.find((s) => s.name === selectedSender));
    };

    const handleUploadOnBack = () => {
        setSender(undefined);
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        const content = await file?.text();
        if (!content || !sender) throw new Error("File, or sender missing");

        const args: OrganizationPublicKeyPostArgs = {
            kid: content,
            sender: sender.name,
        };

        setSubmittedArgs(null);

        await onSubmit(args, file!);

        setSubmittedArgs(args);
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
        } catch (e: any) {
            onError(e);
        }
    };

    const handleSubmissionError = (e: Error) => {
        setSubmittedArgs(undefined);
        onError(
            new Error(`Uploading public key failed. ${e.toString()}`, {
                cause: e,
            }),
        );
    };

    return (
        <GridContainer className="manage-public-key padding-bottom-5 tablet:padding-top-6">
            {!submittedArgs && (
                <h1 className="margin-top-0 margin-bottom-5">
                    Manage public key
                </h1>
            )}
            {isUploadMessageVisible && (
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
                    senders={senders}
                    onSenderSelect={handleSenderSelect}
                />
            )}
            {isPublicKeyFound && <ManagePublicKeyConfigured />}
            {isUploadEnabled && (
                <ErrorBoundary
                    FallbackComponent={ManagePublicKeyUploadError}
                    onError={handleSubmissionError}
                >
                    <ManagePublicKeyUpload
                        onSubmit={handleSubmit}
                        onFileChange={handleFileChange}
                        onBack={sender && handleUploadOnBack}
                        publicKey={publicKey ?? false}
                        file={file}
                    />
                </ErrorBoundary>
            )}
            {isSubmitting && <Spinner />}
            {isPublicKeySubmitted && <ManagePublicKeyUploadSuccess />}
        </GridContainer>
    );
}

export function ManagePublicKeyPage() {
    const { appInsights } = useAppInsightsContext();
    const { user } = useSessionContext();
    const { data: senders } = useOrganizationSenders(user.organization);
    const { data: { keys } = {} } = useOrganizationPublicKeys();
    const { mutateAsync: createKey } = useCreateOrganizationPublicKey();
    const { toast } = useToast();
    const errorHandler = useCallback(
        (e: Error) => {
            toast(e, "error");
        },
        [toast],
    );

    const submitHandler = useCallback(
        async (key: OrganizationPublicKeyPostArgs, file: File) => {
            try {
                await createKey(key);
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
        [appInsights, createKey],
    );

    return (
        <ManagePublicKeyPageBase
            onSubmit={submitHandler}
            keySets={keys}
            senders={senders}
            onError={errorHandler}
        />
    );
}

export default ManagePublicKeyPage;
