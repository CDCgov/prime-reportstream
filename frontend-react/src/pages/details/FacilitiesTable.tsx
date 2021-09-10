import ReportResource from '../../resources/ReportResource';

interface Props {
    /* REQUIRED
    Passing in a report allows this component to map through the facilities property
    to display a row per facility on the FaclitiesTable. */
    report: ReportResource | undefined
}

function FacilitiesTable(props: Props) {
    const { report } = props

    return (
        <section id="facilities" className="grid-container margin-bottom-5">
            <h2>Facilities reporting ({report!.facilities.length})</h2>
            <table
                id="facilitiestable"
                className="usa-table usa-table--borderless prime-table"
                summary="Previous results"
            >
                <thead>
                    <tr>
                        <th scope="col">Organization</th>
                        <th scope="col">Location</th>
                        <th scope="col">CLIA</th>
                        <th scope="col">Total tests</th>
                        <th scope="col">Total positive</th>
                    </tr>
                </thead>
                <tbody id="tBodyFac" className="font-mono-2xs">
                    {report!.facilities.map((facility) => (
                        <tr key={facility.CLIA}>
                            <td>{facility.facility}</td>
                            <td>
                                {facility.location ? facility.location : "-"}
                            </td>
                            <td>{facility.CLIA}</td>
                            <td>{facility.total}</td>
                            <td>
                                {facility.positive ? facility.positive : "-"}
                            </td>
                            <td></td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );
}

export default FacilitiesTable
