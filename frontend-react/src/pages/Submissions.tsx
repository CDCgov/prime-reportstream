import { Suspense, useContext } from "react";
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary, useResource } from "rest-hooks";
import moment from "moment";

import { useOrgName } from "../utils/OrganizationUtils";
import SubmissionsResource from "../resources/SubmissionsResource";
import { GlobalContext } from "../components/GlobalContextProvider";

import { ErrorPage } from "./error/ErrorPage";

const OrgName = () => {
    const orgName: string = useOrgName();
    return (
        <span id="orgName" className="text-normal text-base">
            {orgName}
        </span>
    );
};

function Submissions() {
    // this component will refresh when global context changes (e.g. organization changes)
    const globalState = useContext(GlobalContext);
    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        { organization: globalState.state.organization }
    );
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Submissions | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-top-5">
                <h3 className="margin-bottom-0">
                    <Suspense
                        fallback={
                            <span className="text-normal text-base">
                                Loading Info...
                            </span>
                        }
                    >
                        <OrgName />
                    </Suspense>
                </h3>
                <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
            </section>
            <div className="grid-container usa-section margin-bottom-10">
                <div className="grid-col-12">
                    <h2>Submissions</h2>
                    <table
                        className="usa-table usa-table--borderless prime-table"
                        aria-label="Submission history from the last 30 days"
                    >
                        <thead>
                            <tr>
                                <th scope="col">Date/time submitted</th>
                                <th scope="col">File</th>
                                <th scope="col">Records</th>
                                <th scope="col">Report ID</th>
                                <th scope="col">Warnings</th>
                            </tr>
                        </thead>
                        <tbody id="tBody" className="font-mono-2xs">
                            {submissions.map((s, i) => {
                                return (
                                    <tr key={"submission_" + i}>
                                        <th scope="row">
                                            {moment
                                                .utc(s["createdAt"])
                                                .local()
                                                .format("YYYY-MM-DD HH:mm")}
                                        </th>
                                        <th scope="row"> </th>
                                        {/* File name */}
                                        <th scope="row">
                                            {s["reportItemCount"]}
                                        </th>
                                        <th scope="row">{s["id"]}</th>
                                        <th scope="row">{s["warningCount"]}</th>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </div>
        </NetworkErrorBoundary>
    );
}

export default Submissions;
