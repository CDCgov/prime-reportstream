import React, { Suspense, useRef, useState } from "react";
import { Button, GridContainer, Grid, ModalRef } from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";
import { getStoredOktaToken, getStoredOrg } from "../GlobalContextProvider";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";

import { TextInputComponent } from "./AdminFormEdit";
import { ConfirmSaveSettingModal } from "./CompareJsonModal";

type Props = { orgname: string; sendername: string; action: string };

export function EditSenderSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const sendername = match?.params?.sendername || "";
    const action = match?.params?.action || "";
    const history = useHistory();
    const modalRef = useRef<ModalRef>(null);
    const diffEditorRef = useRef(null);

    const FormComponent = () => {
        const orgSenderSettings: OrgSenderSettingsResource = useResource(
            OrgSenderSettingsResource.detail(),
            { orgname, sendername: sendername }
        );

        const [orgSenderSettingsOldJson, setOrgSenderSettingsOldJson] =
            useState("");
        const [orgSenderSettingsNewJson, setOrgSenderSettingsNewJson] =
            useState("");
        const { fetch: fetchController } = useController();
        const { invalidate } = useController();

        function handleEditorDidMount(editor: null) {
            diffEditorRef.current = editor;
        }

        const ShowCompareConfirm = async () => {
            try {
                // fetch original version
                const accessToken = getStoredOktaToken();
                const organization = getStoredOrg();

                const response = await fetch(
                    `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${orgname}/senders/${sendername}`,
                    {
                        headers: {
                            Authorization: `Bearer ${accessToken}`,
                            Organization: organization!,
                        },
                    }
                );

                const responseBody = await response.json();
                setOrgSenderSettingsOldJson(
                    JSON.stringify(responseBody, jsonSortReplacer, 2)
                );
                setOrgSenderSettingsNewJson(
                    JSON.stringify(orgSenderSettings, jsonSortReplacer, 2)
                );

                modalRef?.current?.toggleModal(undefined, true);
            } catch (e) {
                console.error(e);
            }
        };

        async function resetSenderList() {
            await invalidate(OrgSenderSettingsResource.list(), {
                orgname,
                sendername: match?.params?.sendername,
            });

            return true;
        }

        const saveSenderData = async () => {
            switch (action) {
                case "edit":
                    try {
                        // @ts-ignore
                        const data = diffEditorRef.current
                            .getModifiedEditor()
                            .getValue();

                        await fetchController(
                            OrgSenderSettingsResource.update(),
                            { orgname, sendername: sendername },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${sendername}' has been updated`
                        );
                        await resetSenderList();
                        history.goBack();
                    } catch (e: any) {
                        console.trace(e);

                        showError(
                            `Updating item '${sendername}' failed. ${e.toString()}`
                        );
                        return false;
                    }
                    break;
                case "clone":
                    try {
                        // @ts-ignore
                        const data = diffEditorRef.current
                            .getModifiedEditor()
                            .getValue();
                        await fetchController(
                            // NOTE: this does not use the expected OrgSenderSettingsResource.create() method
                            // due to the endpoint being an 'upsert' (PUT) instead of the expected 'insert' (POST)
                            OrgSenderSettingsResource.update(),
                            { orgname, sendername: orgSenderSettings.name },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${orgSenderSettings.name}' has been created`
                        );
                        await resetSenderList();
                        history.goBack();
                    } catch (e: any) {
                        console.trace(e);

                        showError(
                            `Cloning item '${
                                orgSenderSettings.name
                            }' failed. ${e.toString()}`
                        );
                        return false;
                    }
                    break;
                default:
                    return false;
            }

            return true;
        };

        return (
            <GridContainer>
                <Grid row>
                    <Grid col="fill" className="text-bold">
                        Org name:{" "}
                        {match?.params?.orgname || "missing param 'orgname'"}
                        <br />
                        Sender name:{" "}
                        {match?.params?.sendername ||
                            "missing param 'sendername'"}
                        <br />
                        <br />
                    </Grid>
                </Grid>
                <TextInputComponent
                    fieldname={"name"}
                    label={"Name"}
                    defaultvalue={
                        action === "edit" ? orgSenderSettings.name : ""
                    }
                    savefunc={(v) => (orgSenderSettings.name = v)}
                    disabled={action === "edit"}
                />
                <TextInputComponent
                    fieldname={"format"}
                    label={"Format"}
                    defaultvalue={orgSenderSettings.format}
                    savefunc={(v) => (orgSenderSettings.format = v)}
                />
                <TextInputComponent
                    fieldname={"topic"}
                    label={"Topic"}
                    defaultvalue={orgSenderSettings.topic}
                    savefunc={(v) => (orgSenderSettings.topic = v)}
                />
                <TextInputComponent
                    fieldname={"customerStatus"}
                    label={"Customer Status"}
                    defaultvalue={orgSenderSettings.customerStatus}
                    savefunc={(v) => (orgSenderSettings.customerStatus = v)}
                />
                <TextInputComponent
                    fieldname={"schemaName"}
                    label={"Schema Name"}
                    defaultvalue={orgSenderSettings.schemaName}
                    savefunc={(v) => (orgSenderSettings.schemaName = v)}
                />
                <Grid row>
                    <Button
                        type="button"
                        onClick={async () =>
                            (await resetSenderList()) && history.goBack()
                        }
                    >
                        Cancel
                    </Button>
                    <Button
                        form="edit-setting"
                        type="submit"
                        data-testid="submit"
                        onClick={() => ShowCompareConfirm()}
                    >
                        Save...
                    </Button>
                </Grid>
                <ConfirmSaveSettingModal
                    uniquid={
                        action === "edit" ? sendername : orgSenderSettings.name
                    }
                    onConfirm={saveSenderData}
                    modalRef={modalRef}
                    oldjson={orgSenderSettingsOldJson}
                    newjson={orgSenderSettingsNewJson}
                    handleEditorDidMount={handleEditorDidMount}
                />
            </GridContainer>
        );
    };

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <section className="grid-container margin-bottom-5">
                <Suspense
                    fallback={
                        <span className="text-normal text-base">
                            Loading Sender Settings Info...
                        </span>
                    }
                >
                    <FormComponent />
                </Suspense>
            </section>
        </NetworkErrorBoundary>
    );
}
