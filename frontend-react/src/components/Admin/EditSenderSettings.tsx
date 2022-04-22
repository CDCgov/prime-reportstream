import React, { Suspense, useRef, useState } from "react";
import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { NetworkErrorBoundary, useController, useResource } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";
import {
    getStoredOktaToken,
    getStoredOrg,
} from "../../contexts/SessionStorageTools";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import Spinner from "../Spinner";
import {
    getErrorDetailFromResponse,
    getVersionWarning,
    VersionWarningType,
} from "../../utils/misc";

import { TextAreaComponent, TextInputComponent } from "./AdminFormEdit";
import {
    ConfirmSaveSettingModal,
    ConfirmSaveSettingModalRef,
} from "./CompareJsonModal";

type Props = { orgname: string; sendername: string; action: "edit" | "clone" };

export function EditSenderSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const sendername = match?.params?.sendername || "";
    const action = match?.params?.action || "";

    const FormComponent = () => {
        const history = useHistory();
        const confirmModalRef = useRef<ConfirmSaveSettingModalRef>(null);

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

        async function getLatestSenderResponse() {
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

            return await response.json();
        }

        const ShowCompareConfirm = async () => {
            try {
                // fetch original version
                setLoading(true);
                const latestResponse = await getLatestSenderResponse();
                setOrgSenderSettingsOldJson(
                    JSON.stringify(latestResponse, jsonSortReplacer, 2)
                );
                setOrgSenderSettingsNewJson(
                    JSON.stringify(orgSenderSettings, jsonSortReplacer, 2)
                );

                if (
                    latestResponse?.meta?.version !==
                    orgSenderSettings?.meta?.version
                ) {
                    showError(getVersionWarning(VersionWarningType.POPUP));
                    confirmModalRef?.current?.setWarning(
                        getVersionWarning(
                            VersionWarningType.FULL,
                            latestResponse
                        )
                    );
                    confirmModalRef?.current?.disableSave();
                }

                confirmModalRef?.current?.showModal();
                setLoading(false);
            } catch (e: any) {
                setLoading(false);
                let errorDetail = await getErrorDetailFromResponse(e);
                console.trace(e, errorDetail);
                showError(
                    `Reloading sender '${sendername}' failed with: ${errorDetail}`
                );
                return false;
            }
        };

        async function resetSenderList() {
            await invalidate(OrgSenderSettingsResource.list(), {
                orgname,
                sendername: sendername,
            });

            return true;
        }

        const saveSenderData = async () => {
            try {
                setLoading(true);
                const latestResponse = await getLatestSenderResponse();
                if (
                    latestResponse.meta?.version !==
                    orgSenderSettings?.meta?.version
                ) {
                    // refresh left-side panel in compare modal to make it obvious what has changed
                    setOrgSenderSettingsOldJson(
                        JSON.stringify(latestResponse, jsonSortReplacer, 2)
                    );
                    showError(getVersionWarning(VersionWarningType.POPUP));
                    confirmModalRef?.current?.setWarning(
                        getVersionWarning(
                            VersionWarningType.FULL,
                            latestResponse
                        )
                    );
                    confirmModalRef?.current?.disableSave();
                    return false;
                }

                const data = confirmModalRef?.current?.getEditedText();

                const sendernamelocal =
                    action === "clone" ? orgSenderSettings.name : sendername;

                await fetchController(
                    // NOTE: For 'clone' does not use the expected OrgSenderSettingsResource.create() method
                    // due to the endpoint being an 'upsert' (PUT) instead of the expected 'insert' (POST)
                    OrgSenderSettingsResource.update(),
                    { orgname, sendername: sendernamelocal },
                    data
                );

                showAlertNotification(
                    "success",
                    `Item '${sendernamelocal}' has been saved`
                );
                confirmModalRef?.current?.toggleModal(undefined, false);
                setLoading(false);
                history.goBack();
            } catch (e: any) {
                setLoading(false);
                let errorDetail = await getErrorDetailFromResponse(e);
                console.trace(e, errorDetail);
                showError(
                    `Updating sender '${sendername}' failed with: ${errorDetail}`
                );
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
                        disabled={loading}
                        onClick={() => ShowCompareConfirm()}
                    >
                        Preview...
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
