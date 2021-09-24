import { useResource } from '@rest-hooks/core';
import { useState, useEffect } from 'react';
import AuthResource from '../../resources/AuthResource';
import FacilityResource from '../../resources/FacilityResource';

interface Props {
    /* REQUIRED
    Passing in a report allows this component to map through the facilities property
    to display a row per facility on the FaclitiesTable. */
    reportId: string | undefined
}

function FacilitiesTable(props: Props) {
    const { reportId } = props

    /* DEBUG
       This will be our approach to getting facilities from the API once rest-hooks is 
       configured properly
       >>> Kevin Haube, Sep 24, 2021 */

    // const facilities: FacilityResource[] = useResource(FacilityResource.getFacilities(reportId), {})

    /* INFO
       This is a temporary fix while I work on learning how to configure custom endpoints
       and calls with the rest-hooks library. 
       >>> Kevin Haube, Sep 24, 2021 */
    const [facilities, setFacilicites] = useState([]);
    useEffect(() => {
        fetch(`${AuthResource.getBaseUrl()}/api/history/report/${reportId}/facilities`)
            .then(res => console.log(res))
    }, [])

    return (
        <section id="facilities" className="grid-container margin-bottom-5">
            <h2>Facilities reporting ({facilities.length})</h2>
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
                {/* <tbody id="tBodyFac" className="font-mono-2xs">
                    {facilities.map((facility) => (
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
                </tbody> */}
            </table>
        </section>
    );
}

export default FacilitiesTable
