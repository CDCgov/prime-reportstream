import { useResource } from "rest-hooks";
import ReportResource from "../../resources/ReportResource";
import Summary from "./Summary"
import ReportDetails from './ReportDetails'
import FacilitiesTable from './FacilitiesTable'
import HipaaNotice from "../../components/HipaaNotice";

function urlParseParams(): { [key: string]: string } {
  const query = window.location.search.slice(1);
  const queryMap = {};
  Object.assign(
    queryMap,
    ...query
      .split(',')
      .map((s) => s.split('='))
      .map(([k, v]) => ({ [k]: v })),
  );
  return queryMap;
}

export const Details = () => {
  const queryMap = urlParseParams();
  const reportId = queryMap?.reportId || '';
  const report = useResource(ReportResource.list(), { sortBy: '' }).find((r) => (r.reportId === reportId));

    return (
        <>
            <Summary report={report} />
            <ReportDetails report={report} />
            <FacilitiesTable reportId={report?.reportId} />
            <HipaaNotice />
        </>
    );
};
