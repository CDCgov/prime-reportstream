import React, { Suspense, useRef, useState } from "react";
import { Button, GridContainer, Grid, ModalRef } from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";
import { getStoredOktaToken, getStoredOrg } from "../GlobalContextProvider";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";

import { ConfirmSaveSettingModal } from "./CompareJsonModal";
import {
    CheckboxComponent,
    TextAreaComponent,
    TextInputComponent,
} from "./AdminFormEdit";

type Props = { orgname: string; receivername: string; action: string };

export function EditReceiverSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const receivername = match?.params?.receivername || "";
    const action = match?.params?.action || "";
    const history = useHistory();
    const modalRef = useRef<ModalRef>(null);
    const diffEditorRef = useRef(null);

    const FormComponent = () => {
        const orgReceiverSettings: OrgReceiverSettingsResource = useResource(
            OrgReceiverSettingsResource.detail(),
            { orgname, receivername, action }
        );

        const { fetch: fetchController } = useController();
        const [orgReceiverSettingsOld, setOrgReceiverSettingsOld] =
            useState("");
        const [orgReceiverSettingsNew, setOrgReceiverSettingsNew] =
            useState("");
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
                    `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${orgname}/receivers/${receivername}`,
                    {
                        headers: {
                            Authorization: `Bearer ${accessToken}`,
                            Organization: organization!,
                        },
                    }
                );

                const responseBody = await response.json();
                setOrgReceiverSettingsOld(
                    JSON.stringify(responseBody, jsonSortReplacer, 2)
                );
                setOrgReceiverSettingsNew(
                    JSON.stringify(orgReceiverSettings, jsonSortReplacer, 2)
                );

                modalRef?.current?.toggleModal(undefined, true);
            } catch (e) {
                console.error(e);
            }
        };

        async function resetReceiverList() {
            await invalidate(OrgReceiverSettingsResource.list(), {
                orgname,
                receivername: match?.params?.receivername,
            });

            return true;
        }

        const saveReceiverData = async () => {
            switch (action) {
                case "edit":
                    try {
                        // @ts-ignore
                        const data = diffEditorRef.current
                            .getModifiedEditor()
                            .getValue();

                        await fetchController(
                            OrgReceiverSettingsResource.update(),
                            { orgname, receivername: receivername },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${receivername}' has been updated`
                        );
                        await resetReceiverList();
                        history.goBack();
                    } catch (e: any) {
                        console.trace(e);
                        showError(
                            `Updating item '${receivername}' failed. ${e.toString()}`
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
                            OrgReceiverSettingsResource.update(),
                            { orgname, receivername: orgReceiverSettings.name },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${orgReceiverSettings.name}' has been created`
                        );
                        await resetReceiverList();
                        history.goBack();
                    } catch (e: any) {
                        console.trace(e);
                        showError(
                            `Cloning item '${
                                orgReceiverSettings.name
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
            <GridContainer containerSize={"desktop"}>
                <Grid row>
                    <Grid col="fill" className="text-bold">
                        Org name:{" "}
                        {match?.params?.orgname || "missing param 'orgname'"}
                        <br />
                        Receiver name:{" "}
                        {match?.params?.receivername ||
                            "missing param 'receivername'"}
                        <br />
                        <br />
                    </Grid>
                </Grid>
                <TextInputComponent
                    fieldname={"name"}
                    label={"Name"}
                    defaultvalue={
                        action === "edit" ? orgReceiverSettings.name : ""
                    }
                    savefunc={(v) => (orgReceiverSettings.name = v)}
                    disabled={action === "edit"}
                />
                <TextInputComponent
                    fieldname={"topic"}
                    label={"Topic"}
                    defaultvalue={orgReceiverSettings.topic}
                    savefunc={(v) => (orgReceiverSettings.topic = v)}
                />
                <TextInputComponent
                    fieldname={"customerStatus"}
                    label={"Customer Status"}
                    defaultvalue={orgReceiverSettings.customerStatus}
                    savefunc={(v) => (orgReceiverSettings.customerStatus = v)}
                />
                <TextInputComponent
                    fieldname={"description"}
                    label={"Description"}
                    defaultvalue={orgReceiverSettings.description}
                    savefunc={(v) => (orgReceiverSettings.description = v)}
                />
                <TextAreaComponent
                    fieldname={"translation"}
                    label={"Translation"}
                    defaultvalue={orgReceiverSettings.translation}
                    defaultnullvalue={null}
                    savefunc={(v) => (orgReceiverSettings.translation = v)}
                />
                <TextAreaComponent
                    fieldname={"jurisdictionalFilter"}
                    label={"Jurisdictional Filter"}
                    defaultvalue={orgReceiverSettings.jurisdictionalFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) =>
                        (orgReceiverSettings.jurisdictionalFilter = v)
                    }
                />
                <TextAreaComponent
                    fieldname={"qualityFilter"}
                    label={"Quality Filter"}
                    defaultvalue={orgReceiverSettings.qualityFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) => (orgReceiverSettings.qualityFilter = v)}
                />
                <CheckboxComponent
                    fieldname={"reverseTheQualityFilter"}
                    label={"Reverse the Quality Filter"}
                    defaultvalue={orgReceiverSettings.reverseTheQualityFilter}
                    savefunc={(v) =>
                        (orgReceiverSettings.reverseTheQualityFilter = v)
                    }
                />
                <TextAreaComponent
                    fieldname={"routingFilter"}
                    label={"Routing Filter"}
                    defaultvalue={orgReceiverSettings.routingFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) => (orgReceiverSettings.routingFilter = v)}
                />
                <TextAreaComponent
                    fieldname={"processingModeFilter"}
                    label={"Processing Mode Filter"}
                    defaultvalue={orgReceiverSettings.processingModeFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) =>
                        (orgReceiverSettings.processingModeFilter = v)
                    }
                />
                <CheckboxComponent
                    fieldname={"deidentify"}
                    label={"De-identify"}
                    defaultvalue={orgReceiverSettings.deidentify}
                    savefunc={(v) => (orgReceiverSettings.deidentify = v)}
                />
                <TextAreaComponent
                    fieldname={"timing"}
                    label={"Timing"}
                    defaultvalue={orgReceiverSettings.timing}
                    defaultnullvalue={null}
                    savefunc={(v) => (orgReceiverSettings.timing = v)}
                />
                <TextAreaComponent
                    fieldname={"transport"}
                    label={"Transport"}
                    defaultvalue={orgReceiverSettings.transport}
                    defaultnullvalue={null}
                    savefunc={(v) => (orgReceiverSettings.transport = v)}
                />
                <TextInputComponent
                    fieldname={"externalName"}
                    label={"External Name"}
                    defaultvalue={orgReceiverSettings.externalName}
                    savefunc={(v) => (orgReceiverSettings.externalName = v)}
                />
                <Grid row>
                    <Button
                        type="button"
                        onClick={async () =>
                            (await resetReceiverList()) && history.goBack()
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
                        action === "edit"
                            ? receivername
                            : orgReceiverSettings.name
                    }
                    onConfirm={saveReceiverData}
                    modalRef={modalRef}
                    oldjson={orgReceiverSettingsOld}
                    newjson={orgReceiverSettingsNew}
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
                            Loading Receiver Info...
                        </span>
                    }
                >
                    <FormComponent />
                </Suspense>
            </section>
        </NetworkErrorBoundary>
    );
}
