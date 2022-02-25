import React, { Suspense, useRef } from "react";
import { Button, GridContainer, Grid } from "@trussworks/react-uswds";
import { NetworkErrorBoundary, useController, useResource } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";

import { DiffEditorComponent } from "./DiffEditorComponent";

type Props = {
    orgname: string;
    settingtype: string;
    action: string;
    settingid: string;
    newjson: string;
};

export function CompareSenderSettings({ match }: RouteComponentProps<Props>) {
    const history = useHistory();
    const orgname = match?.params?.orgname || "";
    // const settingtype = match?.params?.settingtype || "";
    // const action = match?.params?.action || "";
    const settingid = match?.params?.settingid || "";

    let newjson = decodeURI(
        match?.params?.newjson.replaceAll("%2F", "/") || ""
    );
    newjson = JSON.parse(newjson);

    const diffEditorRef = useRef(null);

    function handleEditorDidMount(editor: null) {
        diffEditorRef.current = editor;
    }

    const FormComponent = () => {
        const orgSenderSettingsOld = useResource(
            OrgSenderSettingsResource.detail(),
            { orgname, sendername: settingid }
        );

        const { fetch: fetchController } = useController();

        const saveData = async () => {
            try {
                // @ts-ignore
                const data = diffEditorRef.current
                    .getModifiedEditor()
                    .getValue();

                await fetchController(
                    OrgSenderSettingsResource.update(),
                    { orgname, sendername: settingid },
                    data
                );
                showAlertNotification(
                    "success",
                    `Item '${settingid}' has been updated`
                );
                history.goBack();
            } catch (e: any) {
                console.trace(e);

                showError(
                    `Updating item '${settingid}' failed. ${e.toString()}`
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
                        Setting Name:{" "}
                        {match?.params?.settingid ||
                            "missing param 'settingid'"}
                        <br />
                        Setting Type: sender
                        <br />
                        <br />
                    </Grid>
                </Grid>
                <Grid row>
                    <Grid col="fill">
                        <DiffEditorComponent
                            originalCode={JSON.stringify(
                                orgSenderSettingsOld,
                                undefined,
                                2
                            )}
                            // modifiedCode={newjson}
                            modifiedCode={JSON.stringify(
                                newjson,
                                undefined,
                                2
                            ).replace('\\"', '"')}
                            language={"JSON"}
                            mounter={handleEditorDidMount}
                        />
                    </Grid>
                </Grid>
                <Grid row>
                    <Button type="button" onClick={() => history.goBack()}>
                        Cancel
                    </Button>
                    <Button
                        form="edit-setting"
                        type="submit"
                        data-testid="submit"
                        onClick={() => saveData()}
                    >
                        Save
                    </Button>
                </Grid>
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
