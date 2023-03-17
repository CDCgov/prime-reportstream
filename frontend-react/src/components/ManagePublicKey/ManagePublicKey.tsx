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
    validateFileSelectedState,
    ManagePublicKeyAction,
} from "../../hooks/network/ManagePublicKey/ManagePublicKeyHooks";
import { ContentType } from "../../utils/TemporarySettingsAPITypes";

import { ManagePublicKeyChooseSender } from "./ManagePublicKeyChooseSender";
import { ManagePublicKeyUpload } from "./ManagePublicKeyUpload";

const LightbulbIcon = Icon.Lightbulb;

const ManagePublicKeySwitchDisplay = () => {
    const navigate = useNavigate();
    const [action, setAction] = useState(ManagePublicKeyAction.SELECT_SENDER);
    const [fileContent, setFileContent] = useState("");
    const [fileName, setFileName] = useState("");

    // TODO: mocked for now - make the call you need when sending the file
    const { sendFile, isProcessingFile } = {
        // eslint-disable-next-line no-empty-pattern
        sendFile: ({}) => {
            return { fileName: "fileName" };
        },
        isProcessingFile: false,
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
                contentType: ContentType.PEM,
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

        setFileName(file.name);
        setFileContent(content);

        const { fileError } = validateFileSelectedState(file);
        if (fileError) {
            showError(fileError);
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
        <div className="manage-public-key grid-container margin-bottom-5 tablet:margin-top-6">
            <div>
                <h1 className="margin-top-0 margin-bottom-5">
                    Manage Public Key
                </h1>
                <p className="font-sans-md">
                    Send your public key to begin the REST API authentication
                    process.
                </p>
            </div>
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
