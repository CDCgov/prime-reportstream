import { useReportsFacilities } from "../../hooks/network/History/DeliveryHooks";

interface FacilitiesTableProps {
    /* REQUIRED
    Passing in a report allows this component to map through the facilities property
    to display a row per facility on the FaclitiesTable. */
    reportId: string;
}

function FacilitiesTable(props: FacilitiesTableProps) {
    const { reportId }: FacilitiesTableProps = props;
    const { reportFacilities } = useReportsFacilities(reportId);

    return (
        <section id="facilities" className="grid-container margin-bottom-5">
            <h2>Facilities reporting ({reportFacilities?.length || 0})</h2>
            <table
                id="facilitiestable"
                className="usa-table usa-table--borderless prime-table"
                aria-label="Facilities included in this report"
            >
                <thead>
                    <tr>
                        <th scope="col">Facility</th>
                        <th scope="col">Location</th>
                        <th scope="col">CLIA</th>
                        <th scope="col">Total tests</th>
                        <th scope="col">Total positive</th>
                    </tr>
                </thead>
                <tbody id="tBodyFac" className="font-mono-2xs">
                    {reportFacilities?.map((facility, idx) => (
                        <tr key={idx}>
                            <td>{facility.facility}</td>
                            <td>
                                {facility.location ? facility.location : "-"}
                            </td>
                            <td>{facility.CLIA}</td>
                            <td>{facility.total}</td>
                            <td>{facility.positive}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );
}

export default FacilitiesTable;
