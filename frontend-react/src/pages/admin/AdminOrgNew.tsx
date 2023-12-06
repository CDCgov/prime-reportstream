import React, { Suspense, useState } from "react";
import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { NetworkErrorBoundary, useController } from "rest-hooks";
import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet-async";

import { ErrorPage } from "../error/ErrorPage";
import {
    showAlertNotification,
    showError,
} from "../../components/AlertNotifications";
import Spinner from "../../components/Spinner";
import {
    TextAreaComponent,
    TextInputComponent,
} from "../../components/Admin/AdminFormEdit";
import OrganizationResource from "../../resources/OrganizationResource";
import { getErrorDetailFromResponse } from "../../utils/misc";

const fallbackPage = () => <ErrorPage type="page" />;

export function AdminOrgNewPage() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    let orgSetting: object = [];
    let orgName: string = "";

    const { fetch: fetchController } = useController();

    const saveData = async () => {
        setLoading(true);
        try {
            await fetchController(
                OrganizationResource.update(),
                { orgname: orgName },
                orgSetting,
            );
            showAlertNotification(
                "success",
                `Item '${orgName}' has been created`,
            );

            navigate(`/admin/orgsettings/org/${orgName}`);
        } catch (e: any) {
            setLoading(false);
            let errorDetail = await getErrorDetailFromResponse(e);
            console.trace(e, errorDetail);
            showError(`Creating item '${orgName}' failed. ${errorDetail}`);
            return false;
        }

        return true;
    };

    return (
        <NetworkErrorBoundary fallbackComponent={fallbackPage}>
            <Helmet>
                <title>New organization - Admin</title>
            </Helmet>
            <section className="grid-container margin-bottom-5">
                <Suspense
                    fallback={
                        <span className="text-normal text-base">
                            <Spinner />
                        </span>
                    }
                >
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
                            disabled={loading}
                        />
                        <TextAreaComponent
                            fieldname={"orgSetting"}
                            label={"JSON"}
                            savefunc={(v) => (orgSetting = v)}
                            defaultvalue={[]}
                            defaultnullvalue="[]"
                            disabled={loading}
                        />
                        <Grid row>
                            <Button type="button" onClick={() => navigate(-1)}>
                                Cancel
                            </Button>
                            <Button
                                form="create-organization"
                                type="submit"
                                data-testid="submit"
                                onClick={() => saveData()}
                                disabled={loading}
                            >
                                Create
                            </Button>
                        </Grid>
                    </GridContainer>
                </Suspense>
            </section>
        </NetworkErrorBoundary>
    );
}

export default AdminOrgNewPage;
