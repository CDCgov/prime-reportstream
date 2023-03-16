import React, { ReactNode, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    GridContainer,
    SiteAlert,
    Icon,
    Form,
    FormGroup,
    Dropdown,
    Button,
} from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { showError } from "../AlertNotifications";
import Spinner from "../Spinner";
import { USLink } from "../USLink";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { UseOrganizationSenders } from "../../hooks/UseOrganizationSenders";
import { RSSender } from "../../config/endpoints/settings";
import useManagePublicKey, {
    ManagePublicKeyActionType,
} from "../../hooks/network/ManagePublicKey/ManagePublicKeyHooks";
import { ContentType } from "../../utils/TemporarySettingsAPITypes";

import { ManagePublicKeyUpload } from "./ManagePublicKeyUpload";

const LightbulbIcon = Icon.Lightbulb;

// TODO: create common component???
const ManagePublicKeySpinner = ({ message }: { message: ReactNode }) => (
    <div className="grid-col flex-1 display-flex flex-column flex-align-center margin-top-10">
        <div className="grid-row">
            <Spinner />
        </div>
        <div className="text-center">{message}</div>
    </div>
);

const SendersDisplay = ({ senders }: { senders: RSSender[] }) => {
    const navigate = useNavigate();
    const { state, dispatch } = useManagePublicKey();
    const [fileContent, setFileContent] = useState("");
    const [selectedSender, setSelectedSender] = useState(
        senders.length === 1 ? senders[0].name : ""
    );
    const [showUploadDisplay, setShowUploadDisplay] = useState(
        senders.length === 1 ? true : false
    );
    const { fileInputResetValue, fileName, localError } = state;

    const handleSenderSubmit = () => {
        setShowUploadDisplay(true);
    };

    // TODO: mocked for now - make the call you need when sending the file
    const { sendFile, isWorking } = {
        sendFile: ({}) => {
            return { fileName: "fileName" };
        },
        isWorking: false,
    };

    const resetState = () => {
        setFileContent("");
        dispatch({ type: ManagePublicKeyActionType.RESET });
    };

    const handleFileChange = async (
        event: React.ChangeEvent<HTMLInputElement>
    ) => {
        // No file selected
        if (!event?.target?.files?.length) return;

        const file = event.target.files.item(0);
        if (!file) return;

        const content = await file.text();

        setFileContent(content);

        dispatch({
            type: ManagePublicKeyActionType.FILE_SELECTED,
            payload: { file },
        });
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (fileContent.length === 0) {
            showError("No file contents to validate.");
            return;
        }

        // initializes necessary state and sets `isSubmitting`
        dispatch({ type: ManagePublicKeyActionType.PREPARE_FOR_REQUEST });

        try {
            sendFile({
                contentType: ContentType.PEM,
                fileContent: fileContent,
                fileName: fileName,
                selectedSender: selectedSender,
            });
        } catch (e: any) {
            console.trace(e);
            showError(`Uploading public key failed. ${e.toString()}`);
        }
    };

    const handleBack = () => {
        resetState();
        navigate("/resources");
    };

    // TODO: add more logic here to toggle between true/false
    const submitted = false;

    useEffect(() => {
        if (localError) {
            showError(localError);
        }
    }, [localError]);

    return (
        <div className="grid-col-12">
            {senders?.length > 1 && !showUploadDisplay && (
                <Form name="senderSelect" onSubmit={() => handleSenderSubmit()}>
                    <FormGroup>
                        <Dropdown
                            id="senders-dropdown"
                            name="senders-dropdown"
                            onChange={(e) => {
                                setSelectedSender(e.target.value);
                            }}
                        >
                            <option value="">-Select-</option>
                            {senders.map(({ name }) => (
                                <option key={name} value={name}>
                                    {name}
                                </option>
                            ))}
                        </Dropdown>
                        <Button
                            key="submit-sender"
                            type="submit"
                            outline
                            className="padding-bottom-1 padding-top-1"
                            disabled={selectedSender === ""}
                        >
                            Submit
                        </Button>
                    </FormGroup>
                </Form>
            )}
            {showUploadDisplay && isWorking && (
                <ManagePublicKeySpinner message="Processing file..." />
            )}
            {showUploadDisplay && !isWorking && (
                // Do we want to reuse the FileHandlerForm component???
                <ManagePublicKeyUpload
                    handleSubmit={handleSubmit}
                    handleFileChange={handleFileChange}
                    handleBack={handleBack}
                    fileInputResetValue={fileInputResetValue}
                    submitted={submitted}
                    fileName={fileName}
                />
            )}
        </div>
    );
};

export function ManagePublicKey() {
    const { isLoading, senders } = UseOrganizationSenders();

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
            {isLoading && <ManagePublicKeySpinner message="Loading..." />}
            {!isLoading && senders && (
                <SendersDisplay senders={senders}></SendersDisplay>
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
