import React, {Suspense} from "react";
import {Button, Grid, GridContainer} from "@trussworks/react-uswds";
import {NetworkErrorBoundary, useController} from "rest-hooks";
import {useHistory} from "react-router-dom";

import {ErrorPage} from "../error/ErrorPage";
import {showAlertNotification, showError} from "../../components/AlertNotifications";
import Spinner from "../../components/Spinner";

import {TextAreaComponent, TextInputComponent} from "../../components/Admin/AdminFormEdit";
import OrganizationResource from "../../resources/OrganizationResource";


export function AdminOrgNew() {
    const history = useHistory();

    const FormComponent = () => {
        let orgSetting: object = [];
        let orgName: string = "";

        const {fetch: fetchController} = useController();

        const saveData = async () => {
            try {
                await fetchController(
                    OrganizationResource.update(),
                    {orgname: orgName},
                    orgSetting
                );
                showAlertNotification(
                    "success",
                    `Item '${orgName}' has been created`
                );
                history.push(`/admin/orgsettings/org/${orgName}`);
            } catch (e: any) {
                console.trace(e);

                showError(
                    `Creating item '${orgName}' failed. ${e.toString()}`
                );
                return false;
            }

            return true;
        };

        return (
            <GridContainer>
                <Grid row>
                    <Grid col="fill" className="text-bold">
                        Create New Organization
                        <br />
                        <br />
                    </Grid>
                </Grid>
                <TextInputComponent
                    fieldname={"orgName"}
                    label={"Name"}
                    defaultvalue=""
                    savefunc={(v) => (orgName = v)}
                />
                <TextAreaComponent
                    fieldname={"orgSetting"}
                    label={"JSON"}
                    savefunc={(v) => (orgSetting = v)}
                    defaultvalue={[]}
                    defaultnullvalue="[]"
                />
                <Grid row>
                    <Button type="button" onClick={() => history.goBack()}>
                        Cancel
                    </Button>
                    <Button
                        form="create-organization"
                        type="submit"
                        data-testid="submit"
                        onClick={() => saveData()}
                    >
                        Create
                    </Button>
                </Grid>
            </GridContainer>
        );
    };

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page"/>}
        >
            <section className="grid-container margin-bottom-5">
                <Suspense
                    fallback={
                        <span className="text-normal text-base">
                            <Spinner/>
                        </span>
                    }
                >
                    <FormComponent/>
                </Suspense>
            </section>
        </NetworkErrorBoundary>
    );
}
