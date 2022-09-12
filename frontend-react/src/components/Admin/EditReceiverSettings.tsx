import React, { useRef, useState } from "react";
import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { useController, useResource } from "rest-hooks";
import { useNavigate, useParams } from "react-router-dom";

import Title from "../../components/Title";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";
import {
    getStoredOktaToken,
    getStoredOrg,
} from "../../utils/SessionStorageTools";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import {
    getErrorDetailFromResponse,
    getVersionWarning,
    VersionWarningType,
} from "../../utils/misc";
import { EnumTooltip, ObjectTooltip } from "../tooltips/ObjectTooltip";
import {
    getListOfEnumValues,
    SampleTimingObj,
    SampleTranslationObj,
    SampleTransportObject,
} from "../../utils/TemporarySettingsAPITypes";
import { AuthElement } from "../AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";

import {
    ConfirmSaveSettingModal,
    ConfirmSaveSettingModalRef,
} from "./CompareJsonModal";
import {
    CheckboxComponent,
    DropdownComponent,
    TextAreaComponent,
    TextInputComponent,
} from "./AdminFormEdit";
import { AdminFormWrapper } from "./AdminFormWrapper";

type EditReceiverSettingsFormProps = {
    orgname: string;
    receivername: string;
    action: string;
};

const EditReceiverSettingsForm: React.FC<EditReceiverSettingsFormProps> = ({
    orgname,
    receivername,
    action,
}) => {
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
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

    async function getLatestReceiverResponse() {
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

        return await response.json();
    }

    const showCompareConfirm = async () => {
        try {
            // fetch original version
            setLoading(true);
            const latestResponse = await getLatestReceiverResponse();
            setOrgReceiverSettingsOldJson(
                JSON.stringify(latestResponse, jsonSortReplacer, 2)
            );
            setOrgReceiverSettingsNewJson(
                JSON.stringify(orgReceiverSettings, jsonSortReplacer, 2)
            );
            if (latestResponse?.version !== orgReceiverSettings?.version) {
                showError(getVersionWarning(VersionWarningType.POPUP));
                confirmModalRef?.current?.setWarning(
                    getVersionWarning(VersionWarningType.FULL, latestResponse)
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
                `Reloading receiver '${receivername}' failed with: ${errorDetail}`
            );
            return false;
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
            setLoading(true);

            const latestResponse = await getLatestReceiverResponse();
            if (latestResponse.version !== orgReceiverSettings?.version) {
                // refresh left-side panel in compare modal to make it obvious what has changed
                setOrgReceiverSettingsOldJson(
                    JSON.stringify(latestResponse, jsonSortReplacer, 2)
                );
                showError(getVersionWarning(VersionWarningType.POPUP));
                confirmModalRef?.current?.setWarning(
                    getVersionWarning(VersionWarningType.FULL, latestResponse)
                );
                confirmModalRef?.current?.disableSave();
                return false;
            }

            const data = confirmModalRef?.current?.getEditedText();

            const receivernamelocal =
                action === "clone" ? orgReceiverSettings.name : receivername;

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
            navigate(-1);
        } catch (e: any) {
            setLoading(false);
            let errorDetail = await getErrorDetailFromResponse(e);
            console.trace(e, errorDetail);
            showError(
                `Updating receiver '${receivername}' failed with: ${errorDetail}`
            );
            return false;
        }

        return true;
    };

    return (
        <section className="grid-container margin-top-0">
            <GridContainer containerSize={"desktop"}>
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
                <DropdownComponent
                    fieldname={"customerStatus"}
                    label={"Customer Status"}
                    defaultvalue={orgReceiverSettings.customerStatus}
                    savefunc={(v) => (orgReceiverSettings.customerStatus = v)}
                    valuesFrom={"customerStatus"}
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
                    toolTip={<ObjectTooltip obj={new SampleTranslationObj()} />}
                    defaultvalue={orgReceiverSettings.translation}
                    defaultnullvalue={null}
                    savefunc={(v) => (orgReceiverSettings.translation = v)}
                />
                <TextAreaComponent
                    fieldname={"jurisdictionalFilter"}
                    label={"Jurisdictional Filter"}
                    toolTip={
                        <EnumTooltip
                            vals={getListOfEnumValues(
                                "reportStreamFilterDefinition"
                            )}
                        />
                    }
                    defaultvalue={orgReceiverSettings.jurisdictionalFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) =>
                        (orgReceiverSettings.jurisdictionalFilter = v)
                    }
                />
                <TextAreaComponent
                    fieldname={"qualityFilter"}
                    label={"Quality Filter"}
                    toolTip={
                        <EnumTooltip
                            vals={getListOfEnumValues(
                                "reportStreamFilterDefinition"
                            )}
                        />
                    }
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
                    toolTip={
                        <EnumTooltip
                            vals={getListOfEnumValues(
                                "reportStreamFilterDefinition"
                            )}
                        />
                    }
                    defaultvalue={orgReceiverSettings.routingFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) => (orgReceiverSettings.routingFilter = v)}
                />
                <TextAreaComponent
                    fieldname={"processingModeFilter"}
                    label={"Processing Mode Filter"}
                    toolTip={
                        <EnumTooltip
                            vals={getListOfEnumValues(
                                "reportStreamFilterDefinition"
                            )}
                        />
                    }
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
                    toolTip={<ObjectTooltip obj={new SampleTimingObj()} />}
                    defaultvalue={orgReceiverSettings.timing}
                    defaultnullvalue={null}
                    savefunc={(v) => (orgReceiverSettings.timing = v)}
                />
                <TextAreaComponent
                    fieldname={"transport"}
                    label={"Transport"}
                    toolTip={
                        <ObjectTooltip obj={new SampleTransportObject()} />
                    }
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
                <Grid row className="margin-top-2">
                    <Button
                        type="button"
                        onClick={async () =>
                            (await resetReceiverList()) && navigate(-1)
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
        </section>
    );
};

type EditReceiverSettingsProps = {
    orgname: string;
    receivername: string;
    action: "edit" | "clone";
};

export function EditReceiverSettings() {
    const { orgname, receivername, action } =
        useParams<EditReceiverSettingsProps>();

    return (
        <AdminFormWrapper
            header={
                <Title
                    preTitle={`Org name: ${orgname}`}
                    title={`Receiver name: ${receivername}`}
                />
            }
        >
            <EditReceiverSettingsForm
                orgname={orgname || ""}
                receivername={receivername || ""}
                action={action || ""}
            />
        </AdminFormWrapper>
    );
}

export function EditReceiverSettingsWithAuth() {
    return (
        <AuthElement
            element={<EditReceiverSettings />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
