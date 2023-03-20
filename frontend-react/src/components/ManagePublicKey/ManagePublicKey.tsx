import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { GridContainer, Icon, SiteAlert } from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { USLink } from "../USLink";
import Spinner from "../Spinner";
import { showError } from "../AlertNotifications";
import { MemberType } from "../../hooks/UseOktaMemberships";
import {
    validateFileType,
    ManagePublicKeyAction,
    CONTENT_TYPE,
    FORMAT,
    validateFileSize,
} from "../../hooks/network/ManagePublicKey/ManagePublicKeyHooks";

import { ManagePublicKeyChooseSender } from "./ManagePublicKeyChooseSender";
import { ManagePublicKeyUpload } from "./ManagePublicKeyUpload";

const LightbulbIcon = Icon.Lightbulb;

const ManagePublicKeySwitchDisplay = () => {
    const navigate = useNavigate();
    const [action, setAction] = useState(ManagePublicKeyAction.SELECT_SENDER);
    const [fileContent, setFileContent] = useState("");
    const [fileName, setFileName] = useState("");

    // TODO: mocked for now - make the call you need when sending the file
    const { sendFile } = {
        sendFile: (data: {
            contentType?: string;
            fileContent?: string;
            fileName?: string;
        }) => {
            data = { fileName: "filename" };
            return data;
        },
    };

    const onSenderSelect = (selectedSender: string) => {
        if (selectedSender !== "") {
            setAction(ManagePublicKeyAction.SELECT_PUBLIC_KEY);
        }
    };

    const onBack = () => {
        navigate("/resources");
    };

    const onPublicKeySubmit = async (
        event: React.FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        if (fileContent.length === 0) {
            showError("No file contents to validate.");
            return;
        }

        try {
            setAction(ManagePublicKeyAction.SAVE_PUBLIC_KEY);
            sendFile({
                contentType: CONTENT_TYPE,
                fileContent: fileContent,
                fileName: fileName,
            });
            // based on !isProcessing
            setAction(ManagePublicKeyAction.PUBLIC_KEY_SAVED);
        } catch (e: any) {
            console.trace(e);
            showError(`Uploading public key failed. ${e.toString()}`);
            setAction(ManagePublicKeyAction.SELECT_PUBLIC_KEY);
        }
    };

    const onFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
        // No file selected
        if (!event?.target?.files?.length) return;

        const file = event.target.files.item(0);
        if (!file) return;

        const content = await file.text();

        const { fileTypeError } = validateFileType(file, FORMAT);
        if (fileTypeError) {
            showError(fileTypeError);
        }
        const { fileSizeError } = validateFileSize(file);
        if (fileSizeError) {
            showError(fileSizeError);
        }

        if (!fileTypeError && !fileSizeError) {
            setFileName(file.name);
            setFileContent(content);
        }
    };

    switch (action) {
        case ManagePublicKeyAction.SELECT_SENDER:
            return (
                <ManagePublicKeyChooseSender onSenderSelect={onSenderSelect} />
            );
        case ManagePublicKeyAction.SELECT_PUBLIC_KEY:
            return (
                <ManagePublicKeyUpload
                    onPublicKeySubmit={onPublicKeySubmit}
                    onFileChange={onFileChange}
                    onBack={onBack}
                    fileName={fileName}
                />
            );
        case ManagePublicKeyAction.SAVE_PUBLIC_KEY:
            return <Spinner message="Processing file..." />;
        case ManagePublicKeyAction.PUBLIC_KEY_SAVED:
            return <h1> Do something once public key has been saved. </h1>;
        case ManagePublicKeyAction.PUBLIC_KEY_NOT_SAVED:
            return <h1> Error saving public key. </h1>;
        default:
            return null;
    }
};

export function ManagePublicKey() {
    return (
        <GridContainer className="manage-public-key padding-bottom-5 tablet:padding-top-6">
            <h1 className="margin-top-0 margin-bottom-5">Manage Public Key</h1>
            <p className="font-sans-md">
                Send your public key to begin the REST API authentication
                process.
            </p>
            <SiteAlert variant="info" showIcon={false}>
                <LightbulbIcon />
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
