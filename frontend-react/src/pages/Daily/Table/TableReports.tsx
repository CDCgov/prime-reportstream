import { getListOfSenders } from '../../../controllers/ReportController';
import ReportResource from '../../../resources/ReportResource';
import TableButtonGroup from './TableButtonGroup';
import TableReportsData from './TableReportsData';
import { useResource } from 'rest-hooks';
import { useState } from 'react'

/* 
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function TableReports({ sortBy }: { sortBy?: string }) {
    const reports: ReportResource[] = useResource(ReportResource.list(), { sortBy });
    const senders: string[] = Array.from(getListOfSenders(reports));
    const [chosen, setChosen] = useState(senders[0])

    /* This syncs the chosen state from <TableButtonGroup> with the chosen state here */
    const handleCallback = (chosen) => {
        setChosen(chosen)
    }

    return (
        <section className="grid-container margin-top-5">
            <div className="grid-col-12">
                <h2>Test results</h2>
                {
                    /* Button group only shows when there is more than a single sender. */

                    /*  
                        TODO: This may need to be remedied not to show "sendingOrg" as a unique sender
                        as most data input thus far does not have a proper sendingOrg property.
                        >> 09/09/2021 (Kevin Haube)
                    */
                    senders.length > 1 ?
                        <TableButtonGroup senders={senders} chosenCallback={handleCallback} />
                        :
                        null
                }
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
                    <TableReportsData reports={reports.filter(report => report.sendingOrg === chosen)} />
                </table>
                {
                    reports.filter(report => report.sendingOrg === chosen).length === 0 ?
                        <p>No results</p>
                        :
                        null
                }
            </div>
        </section>
    );
}

export default TableReports
