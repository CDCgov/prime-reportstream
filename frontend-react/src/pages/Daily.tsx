import moment from "moment";
import { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { useResource } from "rest-hooks";
import { SpinnerCircularFixed } from "spinners-react";
import ReportResource from "../resources/ReportResource";
import OrganizationResource from "../resources/OrganizationResource";
import { useOktaAuth } from "@okta/okta-react";
import {groupToOrg} from '../webreceiver-utils'


const TableData = ({ sortBy }: { sortBy?: string }) => {
  const [reports] = useResource([ReportResource.list(), {sortBy}] );

  return (
    <tbody id="tBody" className="font-mono-2xs">
      {reports.map((report, idx) => (
        <tr key={idx}>
          <th scope="row">
            <a href={"/report-details?reportId=" + report.reportId} className="usa-link">
              {report.reportId}
            </a>
          </th>
          <th scope="row">
            {moment.utc(report.sent).local().format("YYYY-MM-DD HH:mm")}
          </th>
          <th scope="row">
            {moment.utc(report.expires).local().format("YYYY-MM-DD HH:mm")}
          </th>
          <th scope="row">{report.total}</th>
          <th scope="row">
            <a href={report.reportId} className="usa-link">
              {report.fileType === "HL7_BATCH" ? "HL7(BATCH)" : report.fileType}
            </a>
          </th>
        </tr>
      ))}
    </tbody>
  );
};

const NoData = () => {
  return (
    <tr>
      <th colSpan={5}>No data found</th>
    </tr>
  );
};

const TableReports = () => {
  return (
    <section className="grid-container margin-top-5">
      <div className="grid-col-12">
        <h2>Test results</h2>

        <table
          className="usa-table usa-table--borderless prime-table"
          summary="Previous results"
        >
          <thead>
            <tr>
              <th scope="col">Report Id</th>
              <th scope="col">Date Sent</th>
              <th scope="col">Expires</th>
              <th scope="col">Total tests</th>
              <th scope="col">File</th>
            </tr>
          </thead>
          <Suspense fallback={<tbody><tr><th><SpinnerCircularFixed /></th></tr></tbody>}>
            <NetworkErrorBoundary fallbackComponent={NoData}>
              <TableData />
            </NetworkErrorBoundary>
          </Suspense>
        </table>
      </div>
    </section>
  );
};

const OrgError = () => {
  return (
    <span>OrgError</span>
  )
}

const OrgName = () => {
  const {oktaAuth, authState} = useOktaAuth();

  const organization = groupToOrg( authState.accessToken?.claims.organization[0] )

  const org = useResource(OrganizationResource.detail(), {name: organization} );
  return (
    <span id="orgName" className="text-normal text-base">
      { org.description }
    </span>

  )
}

export const Daily = () => {
  return (
    <>
      <section className="grid-container margin-bottom-5">
        <h3 className="margin-bottom-0">
        <Suspense fallback={<SpinnerCircularFixed />}>
            <NetworkErrorBoundary fallbackComponent={OrgError}>
              <OrgName />
            </NetworkErrorBoundary>
          </Suspense>
        </h3>
        <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
      </section>
      <section className="grid-container margin-top-0">

      </section>
      <TableReports />
    </>
  );
};
