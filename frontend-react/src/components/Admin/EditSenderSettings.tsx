import React, {Suspense, useRef, useState} from "react";
import {Button, GridContainer, Grid, ModalRef} from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { NavLink, RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";

import { TextInputComponent } from "./AdminFormEdit";
import {ConfirmSaveSettingModal} from "./CompareJsonModal";
import {getStoredOktaToken, getStoredOrg} from "../GlobalContextProvider";
import {jsonSortReplacer} from "../../utils/JsonSortReplacer";

type Props = { orgname: string; sendername: string; action: string };

export function EditSenderSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const sendername = match?.params?.sendername || "";
    const action = match?.params?.action || "";
    const history = useHistory();
    const modalRef = useRef<ModalRef>(null);
    const diffEditorRef = useRef(null);
    // const stringify = require('fast-json-stable-stringify');

    // function formatJson(responseBody: any) {
    //     const sorted = stringify(responseBody,
    //         function (a: { key: number; }, b: { key: number; }) {
    //             return a.key < b.key ? -1 : 1;
    //         });
    //     const prettified = JSON.stringify(JSON.parse(sorted), null, 2);
    //     return prettified;
    // }

    const FormComponent = () => {
        const orgSenderSettings: OrgSenderSettingsResource = useResource(
            OrgSenderSettingsResource.detail(),
            { orgname, sendername: sendername }
        );

        const [orgSenderSettingsOld, setOrgSenderSettingsOld] = useState("");

        const { fetch: fetchController } = useController();
        const { invalidate } = useController();

        function handleEditorDidMount(editor: null) {
            diffEditorRef.current = editor;
        }

        const ShowCompareConfirm = async (itemId: string) => {
            try{
                debugger;

                // fetch original version
                const accessToken = getStoredOktaToken();
                const organization = getStoredOrg();

                const response = await fetch(
                    `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${orgname}/senders/${sendername}`,
                    {
                        headers: {
                            Authorization: `Bearer ${accessToken}`,
                            Organization: organization!
                        }

                    }
                );

                const responseBody = await response.json();
                setOrgSenderSettingsOld(JSON.stringify(responseBody, jsonSortReplacer, 2));

                // const prettified = formatJson(responseBody);
                // setOrgSenderSettingsOld(prettified);

                // SetDeleteItemId(itemId);
                modalRef?.current?.toggleModal(undefined, true);
            }
            catch (e) {
                console.error(e);
            }
        };

        const goToCompareSettings = async () => {
            let compareUrl: string = `/admin/comparesettings/org/${orgname}/settingtype/sender/action/${action}/settingid/${
                action === "edit" ? sendername : orgSenderSettings.name
            }/newjson/${encodeURI(JSON.stringify(orgSenderSettings)).replaceAll(
                "/",
                "%2F"
            )}`;

            await invalidate(OrgSenderSettingsResource.detail(), {
                orgname,
                sendername: match?.params?.sendername,
            });

            history.push(compareUrl);
        };

        const saveSenderData = async () => {
            switch (action) {
                case "edit":
                    try {
                        const data = JSON.stringify(orgSenderSettings);
                        await fetchController(
                            OrgSenderSettingsResource.update(),
                            { orgname, sendername: sendername },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${sendername}' has been updated`
                        );
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
                        const data = JSON.stringify(orgSenderSettings);
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
                    <Button type="button" onClick={() => history.goBack()}>
                        Cancel
                    </Button>
                    {/*<Button*/}
                    {/*    form="edit-setting"*/}
                    {/*    type="submit"*/}
                    {/*    data-testid="submit"*/}
                    {/*    onClick={() => saveSenderData()}*/}
                    {/*>*/}
                    {/*    Save*/}
                    {/*</Button>*/}
                    <NavLink
                        className="usa-button"
                        key={`sender-create-link`}
                        onClick={() => goToCompareSettings()}
                        to={"#"}
                    >
                        Save
                    </NavLink>
                    <Button
                        type={"button"}
                        onClick={() =>
                            ShowCompareConfirm(
                                sendername
                            )
                        }
                    >
                        Compare
                    </Button>
                </Grid>
                <ConfirmSaveSettingModal
                    uniquid={sendername}
                    onConfirm={saveSenderData}
                    modalRef={modalRef}
                    oldjson={orgSenderSettingsOld}
                    newjson={JSON.stringify(orgSenderSettings, jsonSortReplacer, 2)}
                    // newjson={formatJson(orgSenderSettings)}
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
