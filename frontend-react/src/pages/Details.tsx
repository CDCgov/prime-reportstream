import moment from 'moment';
import { Suspense } from 'react';
import { NetworkErrorBoundary, useResource } from 'rest-hooks';
import { SpinnerCircularFixed } from 'spinners-react';
import ReportResource from "../resources/ReportResource";

const NoData = () => {
  return ( <span>No data found</span>);
}

const Summary = ( props: {reportId?:String}) => {
  let report = useResource( ReportResource.list(), {sortBy: undefined} )
                .find( (report)=>report.reportId === props.reportId)

  return (
    <section className="grid-container">
      <nav className="usa-breadcrumb usa-breadcrumb--wrap" aria-label="Breadcrumbs">
        <ol className="usa-breadcrumb__list">
          <li className="usa-breadcrumb__list-item">
            <a href="/daily" className="usa-breadcrumb__link" id="orgName">COVID-19</a>
          </li>
          <li className="usa-breadcrumb__list-item usa-current" aria-current="page">
            <span>Report details</span>
          </li>
        </ol>
      </nav>
      <h3 className="margin-top-0 margin-bottom-4">
        <p id="download" className="margin-top-0 margin-bottom-0">
          Report: <span id="report.id">{ report? report.reportId : "this is the report id"}</span>
        </p>
      </h3>
    </section>
  );
};

function useQuery() {
  let query = window.location.search.slice(1);
  const queryMap = {};
  Object.assign(queryMap,...query.split(',').map( s => s.split('=')).map( ([k,v])=> ({ [k]: v})) );
  return queryMap;
}

const ReportDetails = ( props: {reportId?:String}) => {
  let report = useResource( ReportResource.list(), {sortBy: undefined} )
                .find( (report)=>report.reportId === props.reportId)
  return (
    <section className="grid-container margin-top-0 margin-bottom-5">
      <hr />
      <div id="details" className="grid-row grid-gap margin-top-0">
      <div className="tablet:grid-col-3">
                            <h4 className="text-base-darker text-normal margin-bottom-0">Report type</h4>
                            <p className="text-bold margin-top-0">{report!.type}</p>
                            <h4 className="text-base-darker text-normal margin-bottom-0">Report sent</h4>
                            <p className="text-bold margin-top-0">{moment.utc(report!.sent).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
                    </div>
                    <div className="tablet:grid-col-3">
                            <h4 className="text-base-darker text-normal margin-bottom-0">Total tests reported</h4>
                            <p className="text-bold margin-top-0">{report!.total}</p>
                            <h4 className="text-base-darker text-normal margin-bottom-0">Download expires</h4>
                            <p className="text-bold margin-top-0">{moment.utc(report!.expires).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
                    </div>

      </div>
      <hr className="margin-top-3" />
    </section>
  );
};

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const Facilities = (props: {reportId?:String}) => {
  let report = useResource( ReportResource.list(), {sortBy: undefined} )
                .find( (report)=>report.reportId === props.reportId)

  return (
<section id="facilities" className="grid-container margin-bottom-5">
  <h2>Facilities reporting ({report!.facilities.length})</h2>
  <table id="facilitiestable" className="usa-table usa-table--borderless prime-table" summary="Previous results">
    <thead>
      <tr>
        <th scope="col">Organization</th>
        <th scope="col">Location</th>
        <th scope="col">CLIA</th>
        <th scope="col">Total tests</th>
        <th scope="col">Total positive</th>
        <th scope="col">Report history</th>
      </tr>
    </thead>
    <tbody id="tBodyFac" className="font-mono-2xs">
      { report!.facilities.map( facility => 
        <tr>
          <td>{facility.facility}</td>
          <td>{facility.location? facility.location : '-'}</td>
          <td>{facility.CLIA}</td>
          <td>{facility.total}</td>
          <td>{facility.positive? facility.positive : 'Unknown' }</td>
          <td></td>
        </tr>
        )}
    </tbody>
  </table>

</section>
  )                
}

export const Details = ({sortBy}: { sortBy?:String }) => {

  let queryMap = useQuery();

  return (
    <>
      <Suspense fallback={<SpinnerCircularFixed />}>
        <NetworkErrorBoundary fallbackComponent={ NoData }>
          <Summary reportId={queryMap["reportId"] }/>
          <ReportDetails reportId={queryMap["reportId"] }/>
        </NetworkErrorBoundary>
      </Suspense>
    </>
  );
};
