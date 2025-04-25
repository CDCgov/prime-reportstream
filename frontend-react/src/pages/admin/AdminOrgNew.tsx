import { Button, Grid, GridContainer } from "@trussworks/react-uswds";
import { Suspense, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useNavigate } from "react-router-dom";
import { NetworkErrorBoundary, useController } from "rest-hooks";

import { TextAreaComponent, TextInputComponent } from "../../components/Admin/AdminFormEdit";
import Spinner from "../../components/Spinner";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { showToast } from "../../contexts/Toast";
import OrganizationResource from "../../resources/OrganizationResource";
import { getErrorDetailFromResponse } from "../../utils/misc";
import { ErrorPage } from "../error/ErrorPage";

const fallbackPage = () => <ErrorPage type="page" />;

export function AdminOrgNewPage() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const { rsConsole } = useSessionContext();
    let orgSetting: object = [];
    let orgName = "";

    const { fetch: fetchController } = useController();

    const saveData = async () => {
        setLoading(true);
        try {
            await fetchController(OrganizationResource.update(), { orgname: orgName }, orgSetting);
            showToast(`Item '${orgName}' has been created`, "success");

            navigate(`/admin/orgsettings/org/${orgName}`);
        } catch (e: any) {
            setLoading(false);
            const errorDetail = await getErrorDetailFromResponse(e);
            rsConsole.trace(e, errorDetail);
            showToast(`Creating item '${orgName}' failed. ${errorDetail}`, "error");
            return false;
        }

        return true;
    };

    return (
        <NetworkErrorBoundary fallbackComponent={fallbackPage}>
            <Helmet>
                <title>New organization - Admin</title>
                <meta property="og:image" content="/assets/img/opengraph/reportstream.png" />
                <meta
                    property="og:image:alt"
                    content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                />
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
                                onClick={() => void saveData()}
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
