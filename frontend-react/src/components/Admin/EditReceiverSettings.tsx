import React, { Suspense, useRef, useState } from "react";
import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { NetworkErrorBoundary, useController, useResource } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";
import {
    getStoredOktaToken,
    getStoredOrg,
} from "../../contexts/SessionStorageTools";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import Spinner from "../Spinner";
import { getErrorDetail } from "../../utils/misc";

import {
    ConfirmSaveSettingModal,
    ConfirmSaveSettingModalRef,
} from "./CompareJsonModal";
import {
    CheckboxComponent,
    TextAreaComponent,
    TextInputComponent,
} from "./AdminFormEdit";

type Props = {
    orgname: string;
    receivername: string;
    action: "edit" | "clone";
};

export function EditReceiverSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const receivername = match?.params?.receivername || "";
    const action = match?.params?.action || "";

    const FormComponent = () => {
        const [loading, setLoading] = useState(false);
        const history = useHistory();
        const confirmModalRef = useRef<ConfirmSaveSettingModalRef>(null);

        const orgReceiverSettings: OrgReceiverSettingsResource = useResource(
            OrgReceiverSettingsResource.detail(),
            { orgname, receivername, action }
        );

        const { fetch: fetchController } = useController();
        const [orgReceiverSettingsOldJson, setOrgReceiverSettingsOldJson] =
            useState("");
        const [orgReceiverSettingsNewJson, setOrgReceiverSettingsNewJson] =
            useState("");
        const { invalidate } = useController();

        const showCompareConfirm = async () => {
            try {
                // fetch original version
                setLoading(true);
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
                setOrgReceiverSettingsOldJson(
                    JSON.stringify(responseBody, jsonSortReplacer, 2)
                );
                setOrgReceiverSettingsNewJson(
                    JSON.stringify(orgReceiverSettings, jsonSortReplacer, 2)
                );

                confirmModalRef?.current?.showModal();
                setLoading(false);
            } catch (e) {
                setLoading(false);
                console.error(e);
            }
        };

        const resetReceiverList = async () => {
            await invalidate(OrgReceiverSettingsResource.list(), {
                orgname,
                receivername: receivername,
            });

            return true;
        };

        const saveReceiverData = async () => {
            try {
                const data = confirmModalRef?.current?.getEditedText();

                const receivernamelocal =
                    action === "clone"
                        ? orgReceiverSettings.name
                        : receivername;

                setLoading(true);

                await fetchController(
                    OrgReceiverSettingsResource.update(),
                    { orgname, receivername: receivernamelocal },
                    data
                );

                await resetReceiverList();
                showAlertNotification(
                    "success",
                    `Item '${receivername}' has been updated`
                );
                setLoading(false);
                confirmModalRef?.current?.hideModal();
                history.goBack();
            } catch (e: any) {
                let errorDetail = await getErrorDetail(e);
                showError(
                    `Updating receiver '${receivername}' failed with: ${errorDetail}`
                );
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
                        disabled={loading}
                        onClick={showCompareConfirm}
                    >
                        Preview...
                    </Button>
                </Grid>
                <ConfirmSaveSettingModal
                    uniquid={
                        action === "edit"
                            ? receivername
                            : orgReceiverSettings.name
                    }
                    ref={confirmModalRef}
                    onConfirm={saveReceiverData}
                    oldjson={orgReceiverSettingsOldJson}
                    newjson={orgReceiverSettingsNewJson}
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
