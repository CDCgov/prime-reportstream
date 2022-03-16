import React, { Suspense, useRef, useState } from "react";
import { Button, GridContainer, Grid } from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";
import { getStoredOktaToken, getStoredOrg } from "../GlobalContextProvider";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import Spinner from "../Spinner";

import { TextInputComponent, TextAreaComponent } from "./AdminFormEdit";
import {
    ConfirmSaveSettingModal,
    ConfirmSaveSettingModalRef,
} from "./CompareJsonModal";

type Props = { orgname: string; sendername: string; action: string };

export function EditSenderSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const sendername = match?.params?.sendername || "";
    const action = match?.params?.action || "";
    const history = useHistory();
    const confirmModalRef = useRef<ConfirmSaveSettingModalRef>(null);

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
        const [loading, setLoading] = useState(false);

        const ShowCompareConfirm = async () => {
            try {
                // fetch original version
                setLoading(true);
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

                confirmModalRef?.current?.toggleModal(undefined, true);
                setLoading(false);
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
                        const data =
                            confirmModalRef?.current?.editedText ||
                            orgSenderSettingsNewJson;
                        await fetchController(
                            OrgSenderSettingsResource.update(),
                            { orgname, sendername: sendername },
                            data
                        );
                        setTimeout(() => {
                            showAlertNotification(
                                "success",
                                `Item '${sendername}' has been updated`
                            );
                        }, 100);
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
                        const data =
                            confirmModalRef?.current?.editedText ||
                            orgSenderSettingsNewJson;

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
                <TextAreaComponent
                    fieldname={"keys"}
                    label={"Keys"}
                    defaultvalue={orgSenderSettings.keys}
                    defaultnullvalue={""}
                    savefunc={(v) => (orgSenderSettings.keys = v)}
                />
                <TextInputComponent
                    fieldname={"processingType"}
                    label={"Processing Type"}
                    defaultvalue={orgSenderSettings.processingType}
                    savefunc={(v) => (orgSenderSettings.processingType = v)}
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
                        {" "}
                        Save...{" "}
                        <Spinner display={loading} size="insidebutton" />
                    </Button>
                </Grid>
                <ConfirmSaveSettingModal
                    uniquid={
                        action === "edit" ? sendername : orgSenderSettings.name
                    }
                    onConfirm={saveSenderData}
                    ref={confirmModalRef}
                    oldjson={orgSenderSettingsOldJson}
                    newjson={orgSenderSettingsNewJson}
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
                            <Spinner />
                        </span>
                    }
                >
                    <FormComponent />
                </Suspense>
            </section>
        </NetworkErrorBoundary>
    );
}
