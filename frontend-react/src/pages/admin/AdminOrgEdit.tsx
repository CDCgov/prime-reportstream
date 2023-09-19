import React, { Suspense, useRef, useState } from "react";
import { NetworkErrorBoundary, useController, useResource } from "rest-hooks";
import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { useParams } from "react-router-dom";
import { Helmet } from "react-helmet-async";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import OrgSettingsResource from "../../resources/OrgSettingsResource";
import { OrgSenderTable } from "../../components/Admin/OrgSenderTable";
import { OrgReceiverTable } from "../../components/Admin/OrgReceiverTable";
import {
    DropdownComponent,
    TextAreaComponent,
    TextInputComponent,
} from "../../components/Admin/AdminFormEdit";
import {
    showAlertNotification,
    showError,
} from "../../components/AlertNotifications";
import {
    getStoredOktaToken,
    getStoredOrg,
} from "../../utils/SessionStorageTools";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import {
    ConfirmSaveSettingModal,
    ConfirmSaveSettingModalRef,
} from "../../components/Admin/CompareJsonModal";
import { DisplayMeta } from "../../components/Admin/DisplayMeta";
import {
    getErrorDetailFromResponse,
    getVersionWarning,
    VersionWarningType,
} from "../../utils/misc";
import { ObjectTooltip } from "../../components/tooltips/ObjectTooltip";
import { SampleFilterObject } from "../../utils/TemporarySettingsAPITypes";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import config from "../../config";
import { getAppInsightsHeaders } from "../../TelemetryService";
import { USLink } from "../../components/USLink";

const { RS_API_URL } = config;

type AdminOrgEditProps = {
    orgname: string;
};

export function AdminOrgEdit() {
    const { orgname } = useParams<AdminOrgEditProps>();

    const orgSettings: OrgSettingsResource = useResource(
        OrgSettingsResource.detail(),
        { orgname: orgname },
    );
    const confirmModalRef = useRef<ConfirmSaveSettingModalRef>(null);

    const [orgSettingsOldJson, setOrgSettingsOldJson] = useState("");
    const [orgSettingsNewJson, setOrgSettingsNewJson] = useState("");
    const { fetch: fetchController } = useController();
    const [loading, setLoading] = useState(false);

    async function getLatestOrgResponse() {
        const accessToken = getStoredOktaToken();
        const organization = getStoredOrg();

        const response = await fetch(
            `${RS_API_URL}/api/settings/organizations/${orgname}`,
            {
                headers: {
                    ...getAppInsightsHeaders(),
                    Authorization: `Bearer ${accessToken}`,
                    Organization: organization!,
                },
            },
        );

        return await response.json();
    }

    const ShowCompareConfirm = async () => {
        try {
            // fetch original version
            setLoading(true);
            const latestResponse = await getLatestOrgResponse();
            setOrgSettingsOldJson(
                JSON.stringify(latestResponse, jsonSortReplacer, 2),
            );
            setOrgSettingsNewJson(
                JSON.stringify(orgSettings, jsonSortReplacer, 2),
            );

            if (latestResponse?.version !== orgSettings?.version) {
                showError(getVersionWarning(VersionWarningType.POPUP));
                confirmModalRef?.current?.setWarning(
                    getVersionWarning(VersionWarningType.FULL, latestResponse),
                );
                confirmModalRef?.current?.disableSave();
            }

            confirmModalRef?.current?.showModal();
            setLoading(false);
        } catch (e: any) {
            setLoading(false);
            let errorDetail = await getErrorDetailFromResponse(e);
            console.trace(e, errorDetail);
            showError(`Reloading org '${orgname}' failed with: ${errorDetail}`);
            return false;
        }
    };

    const saveOrgData = async () => {
        try {
            const latestResponse = await getLatestOrgResponse();
            if (latestResponse.version !== orgSettings?.version) {
                // refresh left-side panel in compare modal to make it obvious what has changed
                setOrgSettingsOldJson(
                    JSON.stringify(latestResponse, jsonSortReplacer, 2),
                );
                showError(getVersionWarning(VersionWarningType.POPUP));
                confirmModalRef?.current?.setWarning(
                    getVersionWarning(VersionWarningType.FULL, latestResponse),
                );
                confirmModalRef?.current?.disableSave();
                return false;
            }

            const data = confirmModalRef?.current?.getEditedText();
            showAlertNotification("success", `Saving...`);
            await fetchController(
                OrgSettingsResource.update(),
                { orgname },
                data,
            );
            showAlertNotification(
                "success",
                `Item '${orgname}' has been updated`,
            );
            confirmModalRef?.current?.hideModal();
            showAlertNotification("success", `Saved '${orgname}' setting.`);
        } catch (e: any) {
            setLoading(false);
            let errorDetail = await getErrorDetailFromResponse(e);
            console.trace(e, errorDetail);
            showError(`Updating org '${orgname}' failed with: ${errorDetail}`);
            return false;
        }

        return true;
    };

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Admin | Org Edit</title>
            </Helmet>
            <section className="grid-container margin-top-3 margin-bottom-5">
                <h2>
                    Org name: {orgname} {" - "}
                    <USLink
                        href={`/admin/revisionhistory/org/${orgname}/settingtype/organization`}
                    >
                        History
                    </USLink>
                </h2>
            </section>
            <NetworkErrorBoundary
                fallbackComponent={() => <ErrorPage type="message" />}
            >
                <Suspense fallback={<Spinner />}>
                    <section className="grid-container margin-top-0">
                        <GridContainer>
                            <Grid row>
                                <Grid col={3}>Meta:</Grid>
                                <Grid col={9}>
                                    <DisplayMeta metaObj={orgSettings} />
                                    <br />
                                </Grid>
                            </Grid>
                            <TextInputComponent
                                fieldname={"description"}
                                label={"Description"}
                                defaultvalue={orgSettings.description}
                                savefunc={(v) => (orgSettings.description = v)}
                            />
                            <DropdownComponent
                                fieldname={"jurisdiction"}
                                label={"Jurisdiction"}
                                defaultvalue={orgSettings.jurisdiction}
                                savefunc={(v) => (orgSettings.jurisdiction = v)}
                                valuesFrom={"jurisdiction"}
                            />
                            <TextInputComponent
                                fieldname={"countyName"}
                                label={"County Name"}
                                defaultvalue={orgSettings.countyName || null}
                                savefunc={(v) =>
                                    (orgSettings.countyName =
                                        v === "" ? null : v)
                                }
                            />
                            <TextInputComponent
                                fieldname={"stateCode"}
                                label={"State Code"}
                                defaultvalue={orgSettings.stateCode || null}
                                savefunc={(v) =>
                                    (orgSettings.stateCode =
                                        v === "" ? null : v)
                                }
                            />
                            <TextAreaComponent
                                fieldname={"filters"}
                                label={"Filters"}
                                toolTip={
                                    <ObjectTooltip
                                        obj={new SampleFilterObject()}
                                    />
                                }
                                defaultvalue={orgSettings.filters}
                                defaultnullvalue="[]"
                                savefunc={(v) => (orgSettings.filters = v)}
                            />
                            <Grid row>
                                <Button
                                    form="edit-setting"
                                    type="submit"
                                    data-testid="submit"
                                    disabled={loading}
                                    onClick={() => ShowCompareConfirm()}
                                >
                                    Preview save...
                                </Button>
                            </Grid>
                            <ConfirmSaveSettingModal
                                uniquid={orgname || ""}
                                onConfirm={saveOrgData}
                                ref={confirmModalRef}
                                oldjson={orgSettingsOldJson}
                                newjson={orgSettingsNewJson}
                            />
                        </GridContainer>
                        <br />
                    </section>
                    <OrgSenderTable orgname={orgname || ""} />
                    <OrgReceiverTable orgname={orgname || ""} />
                </Suspense>
            </NetworkErrorBoundary>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export function AdminOrgEditWithAuth() {
    return (
        <AuthElement
            element={<AdminOrgEdit />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
