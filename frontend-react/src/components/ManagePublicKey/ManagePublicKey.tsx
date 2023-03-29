import React, { useState } from "react";
import { GridContainer, Icon, SiteAlert } from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { USLink } from "../USLink";
import { showError } from "../AlertNotifications";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { validateFileType, validateFileSize } from "../../utils/FileUtils";

import ManagePublicKeyUpload from "./ManagePublicKeyUpload";

export const CONTENT_TYPE = "application/x-x509-ca-cert";
export const FORMAT = "PEM";

function ManagePublicKeySwitchDisplay() {
    // const [sender, setSender] = useState("");
    const [fileContent, setFileContent] = useState("");
    const [file, setFile] = useState<File | null>(null);
    const [fileSubmitted, setFileSubmitted] = useState(false);

    // TODO: mocked for now - make the call you need when sending the file
    const { sendFile } = {
        sendFile: (data: {
            contentType?: string;
            fileContent?: string;
            file?: File | null;
        }) => {
            data = { file: null };
            return data;
        },
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
            sendFile({
                contentType: CONTENT_TYPE,
                fileContent: fileContent,
                file: file,
            });
            setFileSubmitted(true);
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

    return (
        <>
            {/*Waiting on backend to support this
            {sender.length === 0 && (
                <ManagePublicKeyChooseSender
                    onSenderSelect={(selectedSender: string) =>
                        setSender(selectedSender)
                    }
                />
            )}
            {sender && !fileSubmitted && (*/}
            {!fileSubmitted && (
                <ManagePublicKeyUpload
                    onPublicKeySubmit={handlePublicKeySubmit}
                    onFileChange={handleFileChange}
                    file={file}
                />
            )}
            {fileSubmitted && (
                <h1> Do something once public key has been saved.</h1>
            )}
        </>
    );
}

export function ManagePublicKey() {
    return (
        <GridContainer className="manage-public-key padding-bottom-5 tablet:padding-top-6">
            <h1 className="margin-top-0 margin-bottom-5">Manage Public Key</h1>
            <p className="font-sans-md">
                Send your public key to begin the REST API authentication
                process.
            </p>
            <SiteAlert variant="info" showIcon={false}>
                <Icon.Lightbulb />
                <span className="padding-left-1">
                    If you need more information on generating your public key,
                    reference page 7 in the{" "}
                    <USLink href="/resources/programmers-guide">
                        API Programmer’s Guide.
                    </USLink>
                </span>
            </SiteAlert>
            <ManagePublicKeySwitchDisplay />
        </GridContainer>
    );
}

export const ManagePublicKeyWithAuth = () => (
    <AuthElement
        element={withCatchAndSuspense(<ManagePublicKey />)}
        requiredUserType={MemberType.SENDER}
    />
);
