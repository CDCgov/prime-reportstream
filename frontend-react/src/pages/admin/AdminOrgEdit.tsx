import { Suspense } from "react";
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary, useResource } from "rest-hooks";
import { RouteComponentProps } from "react-router-dom";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import OrgSettingsResource from "../../resources/OrgSettingsResource";

import { OrgSenderTable } from "./Settings/OrgSenderTable";
import { OrgReceiverTable } from "./Settings/OrgReceiverTable";

type AdminOrgEditProps = {
    orgname: string;
};

export function AdminOrgEdit({
    match,
}: RouteComponentProps<AdminOrgEditProps>) {
    const orgname = match?.params?.orgname || "";
    const orgSettings: OrgSettingsResource = useResource(
        OrgSettingsResource.detail(),
        { orgname: orgname }
    );
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Admin | Org Edit | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-bottom-5">
                <h3 className="margin-bottom-0">
                    <Suspense
                        fallback={
                            <span className="text-normal text-base">
                                Loading Info...
                            </span>
                        }
                    >
                        Org name:{" "}
                        {match?.params?.orgname || "missing param 'orgname'"}
                    </Suspense>
                </h3>
            </section>
            <NetworkErrorBoundary
                fallbackComponent={() => <ErrorPage type="message" />}
            >
                <Suspense fallback={<Spinner />}>
                    <section className="grid-container margin-top-0">
                        Description: {orgSettings.description} <br />
                        Jurisdiction: {orgSettings.jurisdiction} <br />
                        County Name: {orgSettings.countyName} <br />
                        State Code: {orgSettings.stateCode} <br />
                        URL: {orgSettings.url} <br />
                        Meta: {JSON.stringify(orgSettings?.meta) || {}} <br />
                        Filter Data: {JSON.stringify(orgSettings?.filter) ||
                            {}}{" "}
                        <br />
                    </section>
                    <OrgSenderTable orgname={orgname} />
                    <OrgReceiverTable orgname={orgname} />
                </Suspense>
            </NetworkErrorBoundary>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}
