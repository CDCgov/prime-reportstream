import { useOktaAuth } from '@okta/okta-react';
import { useState, useEffect, useCallback } from 'react';
import { useResource } from 'rest-hooks';
import Spinner from '../../components/Spinner';
import AuthResource from '../../resources/AuthResource';
import FacilityResource from '../../resources/FacilityResource';
import { getOrganization } from '../../webreceiver-utils';

interface Props {
    /* REQUIRED
    Passing in a report allows this component to map through the facilities property
    to display a row per facility on the FaclitiesTable. */
    reportId: string
}

/* INFO
   This type exists as a part of the tempoarary fix and can be removed after we switch
   back to using useResource() for that call
   >>> Kevin Haube, Sept 27, 2021 */
type Facility = {
    organization: string | undefined
    facility: string | undefined
    location: string | undefined
    CLIA: string | undefined
    positive: string | undefined
    total: string | undefined
}


function FacilitiesTable(props: Props) {
    const { reportId } = props;

    /* INFO
       This is a temporary fix while I work on learning how to configure custom endpoints
       and calls with the rest-hooks library. 
       >>> Kevin Haube, Sep 24, 2021 */

    const [facilities, setFacilicites] = useState<Facility[]>([]);
    const { authState } = useOktaAuth();
    const facilitiesURL = `${AuthResource.getBaseUrl()}/api/history/report/${reportId}/facilities`

    const getFacilities = useCallback(async (fetchURL) => {
        const organization = getOrganization(authState)
        const headers = new Headers({
            'Authorization': `Bearer ${authState?.accessToken?.accessToken}`,
            'Organization': organization!
        });
        const response = await fetch(fetchURL, {
            method: 'GET',
            headers: headers
        })
        const data = await response.json()
        setFacilicites(data)
    }, [authState])

    useEffect(() => {
        getFacilities(facilitiesURL)
    }, [getFacilities, facilitiesURL]);

    if (facilities.length === 0) {
        return (
            <Spinner />
        )
    }

    /* END of temporary fix code */

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
                <tbody id="tBodyFac" className="font-mono-2xs">
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
                </tbody>
            </table >
        </section >
    );
}

export default FacilitiesTable
