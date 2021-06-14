import moment from "moment";
import { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { useResource } from "rest-hooks";
import { SpinnerCircularFixed } from "spinners-react";
import ReportResource from "../resources/ReportResource";
import LineChart from "react-chartjs-2";
import CardResource from "../resources/CardResource";

const ChartData = () => {

  let card = useResource( CardResource.detail(), {id: "summary-tests"});

  //let card = cards[0];

  var labels: Array<String> = [];
  for (var i = 30; i >= 0; i--) {
    labels.push(moment().subtract(i, "days").format("YYYY-MM-DD"));
  }

  var data = {
    labels: labels,
    datasets: [
      {
        data: card.data,
        borderColor: "#4682B4",
        backgroundColor: "#B0C4DE",
        fill: "origin",
        borderJoinStyle: "round",
      },
    ],
  };

  var options = {
    plugins: { legend: { display: false } },
    scales: {
      y: { display: false },
      x: { display: false }
    },
  };

  return (
    <div className="tablet:grid-col-6">
      <div className="usa-card__container">
        <div className="usa-card__body">
          <h4 className="text-base margin-bottom-0">{card.title}</h4>
          <h4 className="text-bold margin-top-0">{card.subtitle}</h4>
          <h4 className="text-base margin-bottom-0">Last 30 days (average)</h4>
          <p className="text-bold margin-top-0">{card.last.toFixed(2)}</p>
          <LineChart type="line" data={data} options={options} />
        </div>
      </div>
    </div>
  );
};

const TableData = ({ sortBy }: { sortBy?: string }) => {
  const [reports] = useResource(
    [ReportResource.list(), {sortBy}] );

  return (
    <tbody id="tBody" className="font-mono-2xs">
      {reports.map((report, idx) => (
        <tr key={idx}>
          <th scope="row">
            <a
              href={"/report-details?reportId=" + report.reportId}
              className="usa-link"
            >
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

export const Daily = () => {
  return (
    <>
      <section className="grid-container margin-bottom-5">
        <h3 className="margin-bottom-0">
          <span id="orgName" className="text-normal text-base">
            Pima County, Arizona PHD
          </span>
        </h3>
        <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
      </section>
      <section className="grid-container margin-top-0">
        <div id="cards" className="grid-row margin-top-0">
        <Suspense fallback={<SpinnerCircularFixed />}>
            <NetworkErrorBoundary fallbackComponent={NoData}>
              <ChartData />
            </NetworkErrorBoundary>
        </Suspense>
        </div>
      </section>
      <TableReports />
    </>
  );
};
