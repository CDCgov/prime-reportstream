import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { FC, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useController, useResource } from "rest-hooks";

import { CheckboxComponent, DropdownComponent, TextAreaComponent, TextInputComponent } from "./AdminFormEdit";
import { AdminFormWrapper } from "./AdminFormWrapper";
import { ConfirmSaveSettingModal, ConfirmSaveSettingModalRef } from "./CompareJsonModal";
import Title from "../../components/Title";
import config from "../../config";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { showToast } from "../../contexts/Toast";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import { getErrorDetailFromResponse, getVersionWarning, VersionWarningType } from "../../utils/misc";
import {
    getListOfEnumValues,
    SampleTimingObj,
    SampleTranslationObj,
    SampleTransportObject,
} from "../../utils/TemporarySettingsAPITypes";
import { ModalConfirmDialog, ModalConfirmRef } from "../ModalConfirmDialog";
import { EnumTooltip, ObjectTooltip } from "../tooltips/ObjectTooltip";

const { RS_API_URL } = config;

interface EditReceiverSettingsFormProps {
    orgname: string;
    receivername: string;
    action: "edit" | "clone";
}

const EditReceiverSettingsForm: FC<EditReceiverSettingsFormProps> = ({ orgname, receivername, action }) => {
    const { properties } = useAppInsightsContext();
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const { activeMembership, authState } = useSessionContext();
    const confirmModalRef = useRef<ConfirmSaveSettingModalRef>(null);

    const orgReceiverSettings: OrgReceiverSettingsResource = useResource(OrgReceiverSettingsResource.detail(), {
        orgname,
        receivername,
        action,
    });

    const { fetch: fetchController } = useController();
    const [orgReceiverSettingsOldJson, setOrgReceiverSettingsOldJson] = useState("");
    const [orgReceiverSettingsNewJson, setOrgReceiverSettingsNewJson] = useState("");
    const { invalidate } = useController();

    const modalRef = useRef<ModalConfirmRef>(null);
    const ShowDeleteConfirm = (deleteItemId: string) => {
        modalRef?.current?.showModal({
            title: "Confirm Delete",
            message: "Deleting a setting will only mark it deleted. It can be accessed via the revision history",
            okButtonText: "Delete",
            itemId: deleteItemId,
        });
    };
    const doDelete = async (deleteItemId: string) => {
        try {
            await fetchController(OrgReceiverSettingsResource.deleteSetting(), {
                orgname: orgname,
                receivername: deleteItemId,
            });

            showToast(`Item '${deleteItemId}' has been deleted`, "success");

            // navigate back to list since this item was just deleted
            navigate(-1);
            return true;
        } catch (e: any) {
            showToast(`Deleting item '${deleteItemId}' failed. ${e.toString()}`, "error");
            return false;
        }
    };

    async function getLatestReceiverResponse() {
        const accessToken = authState.accessToken?.accessToken;
        const organization = activeMembership?.parsedName;

        const response = await fetch(`${RS_API_URL}/api/settings/organizations/${orgname}/receivers/${receivername}`, {
            headers: {
                "x-ms-session-id": properties.context.getSessionId(),
                Authorization: `Bearer ${accessToken}`,
                Organization: organization!,
            },
        });

        return await response.json();
    }

    const showCompareConfirm = async () => {
        try {
            // fetch original version
            setLoading(true);
            const latestResponse = await getLatestReceiverResponse();
            setOrgReceiverSettingsOldJson(JSON.stringify(latestResponse, jsonSortReplacer, 2));
            setOrgReceiverSettingsNewJson(JSON.stringify(orgReceiverSettings, jsonSortReplacer, 2));
            if (action === "edit" && latestResponse?.version !== orgReceiverSettings?.version) {
                showToast(getVersionWarning(VersionWarningType.POPUP), "error");
                confirmModalRef?.current?.setWarning(getVersionWarning(VersionWarningType.FULL, latestResponse));
                confirmModalRef?.current?.disableSave();
            }

            confirmModalRef?.current?.showModal();
            setLoading(false);
        } catch (e: any) {
            setLoading(false);
            const errorDetail = await getErrorDetailFromResponse(e);
            showToast(`Reloading receiver '${receivername}' failed with: ${errorDetail}`, "error");
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
                setOrgReceiverSettingsOldJson(JSON.stringify(latestResponse, jsonSortReplacer, 2));
                showToast(getVersionWarning(VersionWarningType.POPUP), "error");
                confirmModalRef?.current?.setWarning(getVersionWarning(VersionWarningType.FULL, latestResponse));
                confirmModalRef?.current?.disableSave();
                return false;
            }

            const data = confirmModalRef?.current?.getEditedText();

            const receivernamelocal = action === "clone" ? orgReceiverSettings.name : receivername;

            await fetchController(
                OrgReceiverSettingsResource.update(),
                { orgname, receivername: receivernamelocal },
                data,
            );

            await resetReceiverList();
            showToast(`Item '${receivername}' has been updated`, "success");
            setLoading(false);
            confirmModalRef?.current?.hideModal();
            navigate(-1);
        } catch (e: any) {
            setLoading(false);
            const errorDetail = await getErrorDetailFromResponse(e);
            showToast(`Updating receiver '${receivername}' failed with: ${errorDetail}`, "error");
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
                    defaultvalue={action === "edit" ? orgReceiverSettings.name : ""}
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
                <DropdownComponent
                    fieldname={"timeZone"}
                    label={"Time Zone"}
                    defaultvalue={orgReceiverSettings.timeZone}
                    savefunc={(v) => (orgReceiverSettings.timeZone = v)}
                    valuesFrom={"timeZone"}
                />
                <DropdownComponent
                    fieldname={"dateTimeFormat"}
                    label={"Date Time Format"}
                    defaultvalue={orgReceiverSettings.dateTimeFormat}
                    savefunc={(v) => (orgReceiverSettings.dateTimeFormat = v)}
                    valuesFrom={"dateTimeFormat"}
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
                    toolTip={<EnumTooltip vals={getListOfEnumValues("reportStreamFilterDefinition")} />}
                    defaultvalue={orgReceiverSettings.jurisdictionalFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) => (orgReceiverSettings.jurisdictionalFilter = v)}
                />
                <TextAreaComponent
                    fieldname={"qualityFilter"}
                    label={"Quality Filter"}
                    toolTip={<EnumTooltip vals={getListOfEnumValues("reportStreamFilterDefinition")} />}
                    defaultvalue={orgReceiverSettings.qualityFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) => (orgReceiverSettings.qualityFilter = v)}
                />
                <CheckboxComponent
                    fieldname={"reverseTheQualityFilter"}
                    label={"Reverse the Quality Filter"}
                    defaultvalue={orgReceiverSettings.reverseTheQualityFilter}
                    savefunc={(v) => (orgReceiverSettings.reverseTheQualityFilter = v)}
                />
                <TextAreaComponent
                    fieldname={"routingFilter"}
                    label={"Routing Filter"}
                    toolTip={<EnumTooltip vals={getListOfEnumValues("reportStreamFilterDefinition")} />}
                    defaultvalue={orgReceiverSettings.routingFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) => (orgReceiverSettings.routingFilter = v)}
                />
                <TextAreaComponent
                    fieldname={"processingModeFilter"}
                    label={"Processing Mode Filter"}
                    toolTip={<EnumTooltip vals={getListOfEnumValues("reportStreamFilterDefinition")} />}
                    defaultvalue={orgReceiverSettings.processingModeFilter}
                    defaultnullvalue="[]"
                    savefunc={(v) => (orgReceiverSettings.processingModeFilter = v)}
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
                    toolTip={<ObjectTooltip obj={new SampleTransportObject()} />}
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
                    <Grid col={6}>
                        {action === "edit" ? (
                            <Button
                                type={"button"}
                                secondary={true}
                                data-testid={"receiverSettingDeleteButton"}
                                onClick={() => ShowDeleteConfirm(receivername)}
                            >
                                Delete...
                            </Button>
                        ) : null}
                    </Grid>
                    <Grid col={6} className={"text-right"}>
                        <Button
                            type="button"
                            outline={true}
                            onClick={() => void resetReceiverList().then(() => navigate(-1))}
                        >
                            Cancel
                        </Button>
                        <Button
                            form="edit-setting"
                            type="submit"
                            data-testid="submit"
                            disabled={loading}
                            onClick={() => void showCompareConfirm()}
                        >
                            Edit json and save...
                        </Button>
                    </Grid>
                </Grid>
                <ConfirmSaveSettingModal
                    uniquid={action === "edit" ? receivername : orgReceiverSettings.name}
                    ref={confirmModalRef}
                    onConfirm={() => void saveReceiverData()}
                    oldjson={orgReceiverSettingsOldJson}
                    newjson={orgReceiverSettingsNewJson}
                />
            </GridContainer>
            <ModalConfirmDialog
                id={"deleteConfirm"}
                onConfirm={(id) => void doDelete(id)}
                ref={modalRef}
            ></ModalConfirmDialog>
        </section>
    );
};

export interface EditReceiverSettingsParams extends Record<string, string> {
    orgname: string;
    receivername: string;
    action: "edit" | "clone";
}

export function EditReceiverSettingsPage() {
    const { orgname, receivername, action } = useParams<EditReceiverSettingsParams>();

    return (
        <AdminFormWrapper header={<Title preTitle={`Org name: ${orgname}`} title={`Receiver name: ${receivername}`} />}>
            <EditReceiverSettingsForm
                orgname={orgname ?? ""}
                receivername={receivername ?? ""}
                action={action ?? "edit"}
            />
        </AdminFormWrapper>
    );
}

export default EditReceiverSettingsPage;
