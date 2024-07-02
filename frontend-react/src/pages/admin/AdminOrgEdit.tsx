import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { Suspense, useRef, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router-dom";
import { NetworkErrorBoundary, useController, useResource } from "rest-hooks";

import {
    DropdownComponent,
    TextAreaComponent,
    TextInputComponent,
} from "../../components/Admin/AdminFormEdit";
import {
    ConfirmSaveSettingModal,
    ConfirmSaveSettingModalRef,
} from "../../components/Admin/CompareJsonModal";
import { DisplayMeta } from "../../components/Admin/DisplayMeta";
import { OrgReceiverTable } from "../../components/Admin/OrgReceiverTable";
import { OrgSenderTable } from "../../components/Admin/OrgSenderTable";
import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ObjectTooltip } from "../../components/tooltips/ObjectTooltip";
import { USLink } from "../../components/USLink";
import config from "../../config";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { useToast } from "../../contexts/Toast";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import OrgSettingsResource from "../../resources/OrgSettingsResource";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import {
    getErrorDetailFromResponse,
    getVersionWarning,
    VersionWarningType,
} from "../../utils/misc";
import { SampleFilterObject } from "../../utils/TemporarySettingsAPITypes";
import { ErrorPage } from "../error/ErrorPage";

const { RS_API_URL } = config;

interface AdminOrgEditProps {
    orgname: string;
    [k: string]: string | undefined;
}

export function AdminOrgEditPage() {
    const { toast: showAlertNotification } = useToast();
    const { properties } = useAppInsightsContext();
    const { orgname } = useParams<AdminOrgEditProps>();
    const { activeMembership, authState } = useSessionContext();

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
        const accessToken = authState.accessToken?.accessToken;
        const organization = activeMembership?.parsedName;

        const response = await fetch(
            `${RS_API_URL}/api/settings/organizations/${orgname}`,
            {
                headers: {
                    "x-ms-session-id": properties.context.getSessionId(),
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
                showAlertNotification(
                    getVersionWarning(VersionWarningType.POPUP),
                    "error",
                );
                confirmModalRef?.current?.setWarning(
                    getVersionWarning(VersionWarningType.FULL, latestResponse),
                );
                confirmModalRef?.current?.disableSave();
            }

            confirmModalRef?.current?.showModal();
            setLoading(false);
        } catch (e: any) {
            setLoading(false);
            const errorDetail = await getErrorDetailFromResponse(e);
            showAlertNotification(
                new Error(
                    `Reloading org '${orgname}' failed with: ${errorDetail}`,
                    { cause: e },
                ),
                "error",
            );
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
                showAlertNotification(
                    getVersionWarning(VersionWarningType.POPUP),
                    "error",
                );
                confirmModalRef?.current?.setWarning(
                    getVersionWarning(VersionWarningType.FULL, latestResponse),
                );
                confirmModalRef?.current?.disableSave();
                return false;
            }

            const data = confirmModalRef?.current?.getEditedText();
            showAlertNotification(`Saving...`, "success");
            await fetchController(
                OrgSettingsResource.update(),
                { orgname },
                data,
            );
            showAlertNotification(
                `Item '${orgname}' has been updated`,
                "success",
            );
            confirmModalRef?.current?.hideModal();
            showAlertNotification(`Saved '${orgname}' setting.`, "success");
        } catch (e: any) {
            setLoading(false);
            const errorDetail = await getErrorDetailFromResponse(e);
            showAlertNotification(
                new Error(
                    `Updating org '${orgname}' failed with: ${errorDetail}`,
                    { cause: e },
                ),
                "error",
            );
            return false;
        }

        return true;
    };

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Organization edit - Admin</title>
                <meta
                    property="og:image"
                    content="/assets/img/opengraph/reportstream.png"
                />
                <meta
                    property="og:image:alt"
                    content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                />
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
                                defaultvalue={orgSettings.countyName ?? null}
                                savefunc={(v) =>
                                    (orgSettings.countyName =
                                        v === "" ? null : v)
                                }
                            />
                            <TextInputComponent
                                fieldname={"stateCode"}
                                label={"State Code"}
                                defaultvalue={orgSettings.stateCode ?? null}
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
                                    onClick={() => void ShowCompareConfirm()}
                                >
                                    Preview save...
                                </Button>
                            </Grid>
                            <ConfirmSaveSettingModal
                                uniquid={orgname ?? ""}
                                onConfirm={() => void saveOrgData()}
                                ref={confirmModalRef}
                                oldjson={orgSettingsOldJson}
                                newjson={orgSettingsNewJson}
                            />
                        </GridContainer>
                        <br />
                    </section>
                    <OrgSenderTable orgname={orgname ?? ""} />
                    <OrgReceiverTable orgname={orgname ?? ""} />
                </Suspense>
            </NetworkErrorBoundary>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export default AdminOrgEditPage;
