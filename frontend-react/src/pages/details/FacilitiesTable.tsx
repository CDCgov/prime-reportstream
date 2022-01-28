import { useOktaAuth } from "@okta/okta-react";
import { useEffect, useState } from "react";

import FacilityResource from "../../resources/FacilityResource";
import { useGlobalContext } from "../../components/GlobalContextProvider";

interface FacilitiesTableProps {
    /* REQUIRED
    Passing in a report allows this component to map through the facilities property
    to display a row per facility on the FaclitiesTable. */
    reportId: string;
}

function FacilitiesTable(props: FacilitiesTableProps) {
    const { authState } = useOktaAuth();
    const { state } = useGlobalContext();

    const [facilities, setFacilities] = useState<FacilityResource[]>();
    const { reportId }: FacilitiesTableProps = props;

    useEffect(() => {
        fetch(
            `${process.env.REACT_APP_BACKEND_URL}/api/history/report/${reportId}/facilities`,
            {
                headers: {
                    Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                    Organization: state.organization!,
                },
            }
        )
            .then((response) => response.json())
            .then((data) => setFacilities(data));
    });

    return (
        <section id="facilities" className="grid-container margin-bottom-5">
            <h2>Facilities reporting ({facilities?.length})</h2>
            <table
                id="facilitiestable"
                className="usa-table usa-table--borderless prime-table"
                aria-label="Facilities included in this report"
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
                    {facilities?.map((facility) => (
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
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );
}

export default FacilitiesTable;
