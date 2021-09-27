import HipaaNotice from "../../components/HipaaNotice";
import TableReports from "./Table/TableReports";
import { Suspense } from "react";
import Spinner from "../../components/Spinner";
import { useOrgName } from "../../controllers/OrganizationController";

const OrgName = () => {
    const orgName: string = useOrgName();
    return (
        <span id="orgName" className="text-normal text-base">
            {orgName}
        </span>
    );
};

function Daily() {

    return (
        <>
            <section className="grid-container margin-bottom-5">
                <Suspense fallback={<span className="text-normal text-base">Loading Info...</span>}>
                    <h3 className="margin-bottom-0">
                        <OrgName />
                    </h3>

                </Suspense>
                <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
            </section>
            <Suspense fallback={<Spinner />}>
                <section className="grid-container margin-top-0"></section>
                <TableReports />
            </Suspense>
            <HipaaNotice />
        </>
    );
};

export default Daily