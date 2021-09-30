import { useResource } from "rest-hooks";
import ReportResource from "../../resources/ReportResource";
import Summary from "./Summary"
import ReportDetails from './ReportDetails'
import FacilitiesTable from './FacilitiesTable'
import HipaaNotice from "../../components/HipaaNotice";
import { Suspense } from "react";
import Spinner from "../../components/Spinner";

function useQuery() {
    let query = window.location.search.slice(1);
    const queryMap = {};
    Object.assign(
        queryMap,
        ...query
            .split(",")
            .map((s) => s.split("="))
            .map(([k, v]) => ({ [k]: v }))
    );
    return queryMap;
}

const DetailsContent = () => {
    let queryMap = useQuery();
    let reportId = queryMap["reportId"];
    let report = useResource(ReportResource.list(), { sortBy: undefined }).find(
        (r) => r.reportId === reportId
    );

    return (
        <>
            <Summary report={report} />
            <ReportDetails report={report} />
            <FacilitiesTable reportId={report?.reportId} />
            <HipaaNotice />
        </>
    );
};

export const Details = () => {
    return (
        <Suspense fallback={<Spinner fullPage />}>
            <DetailsContent />
        </Suspense>
    )
}
