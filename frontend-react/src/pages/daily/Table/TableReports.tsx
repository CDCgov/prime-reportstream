import { useResource } from "rest-hooks";
import { SetStateAction, useState } from "react";

import { getUniqueReceiverSvc } from "../../../utils/ReportUtils";
import ReportResource from "../../../resources/ReportResource";

import TableButtonGroup from "./TableButtonGroup";
import TableReportsData from "./TableReportsData";

/* 
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function TableReports({ sortBy }: { sortBy?: string }) {
    const reports: ReportResource[] = useResource(ReportResource.list(), {
        sortBy,
    });
    const receiverSVCs: string[] = Array.from(getUniqueReceiverSvc(reports));
    const [chosen, setChosen] = useState(receiverSVCs[0]);

    /* This syncs the chosen state from <TableButtonGroup> with the chosen state here */
    const handleCallback = (chosen: SetStateAction<string>) => {
        setChosen(chosen);
    };

    return (
        <section className="margin-top-5">
            <div className="grid-col-12">
                <h2>Test results</h2>
                {
                    /* Button group only shows when there is more than a single sender. */
                    receiverSVCs.length > 1 ? (
                        <TableButtonGroup
                            senders={receiverSVCs}
                            chosenCallback={handleCallback}
                        />
                    ) : null
                }
                <table
                    className="usa-table usa-table--borderless prime-table"
                    aria-label="Test results reported in the last 30 days"
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
                    <TableReportsData
                        reports={reports.filter(
                            (report) => report.receivingOrgSvc === chosen
                        )}
                    />
                </table>
                {reports.filter((report) => report.receivingOrgSvc === chosen)
                    .length === 0 ? (
                    <p>No results</p>
                ) : null}
            </div>
        </section>
    );
}

export default TableReports;
