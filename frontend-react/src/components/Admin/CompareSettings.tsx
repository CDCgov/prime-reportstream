import React, { Suspense } from "react";
import { Button, GridContainer, Grid } from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";
// import { diff as DiffEditor } from "react-ace";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";

// import { render } from "react-dom";
// import "ace-builds/src-noconflict/theme-github";
// import "ace-builds/webpack-resolver";
// import {TextAreaComponent} from "./AdminFormEdit";
// import { TextInputComponent } from "./AdminFormEdit";

type Props = {
    orgname: string;
    sendername: string;
    receivername: string;
    action: string;
};

export function CompareSettings({ match }: RouteComponentProps<Props>) {
    const history = useHistory();
    //     const defaultVal = `{
    //     "name": "ignore",
    //     "description": "FOR TESTING ONLY sdsd",
    //     "jurisdiction": "FEDERAL",
    //     "stateCode": null,
    //     "countyName": null,
    //     "filters": [
    //         {
    //             "topic": "covid-19",
    //             "jurisdictionalFilter": [
    //                 "matches(ordering_facility_state, IG)"
    //             ],
    //             "qualityFilter": null,
    //             "routingFilter": null,
    //             "processingModeFilter": null
    //         }
    //     ],
    //     "meta": {
    //         "version": 16,
    //         "createdBy": "local@test.com",
    //         "createdAt": "2022-02-03T18:12:44.606332Z"
    //     }
    // }`;
    // const [state, setState] = useState([defaultVal, defaultVal]);
    // const state2: string[] = useRef([defaultVal,defaultVal]);
    const orgname = match?.params?.orgname || "";
    const sendername = match?.params?.sendername || "";
    // const receivername = match?.params?.receivername || "";
    const action = match?.params?.action || "";

    // setState([defaultVal,defaultVal]);

    const FormComponent = () => {
        const orgSenderSettings: OrgSenderSettingsResource = useResource(
            OrgSenderSettingsResource.detail(),
            { orgname, sendername: sendername, action: action }
        );

        const { fetch: fetchController } = useController();
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

        // function onChange(value: string[]) {
        //     // debugger;
        //     // setState(value);
        //     // state2[0] = value[0];
        //     // state2[1] = value[1];
        // }

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
                {/*<TextAreaComponent*/}
                {/*    fieldname={"filters"}*/}
                {/*    label={"Filters"}*/}
                {/*    defaultvalue={orgSettings.filters}*/}
                {/*    savefunc={(v) => (orgSettings.filters = v)}*/}
                {/*/>*/}
                <Grid row>
                    <Button type="button" onClick={() => history.goBack()}>
                        Cancel
                    </Button>
                    <Button
                        form="edit-setting"
                        type="submit"
                        data-testid="submit"
                        onClick={() => saveSenderData()}
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
