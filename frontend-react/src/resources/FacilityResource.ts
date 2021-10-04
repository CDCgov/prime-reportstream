import { Endpoint } from '@rest-hooks/endpoint';
import { Resource } from '@rest-hooks/rest';
import AuthResource from './AuthResource';

export default class FacilityResource extends AuthResource {
    readonly organization: string | undefined = '';
    readonly facility: string | undefined = '';
    readonly location: string | undefined = '';
    readonly CLIA: string | undefined = '';
    readonly positive: string | undefined = '';
    readonly total: string | undefined = '';

    pk(){
        return this.CLIA;
    }

    static urlRoot = `${AuthResource.getBaseUrl()}/api/history/report`;

    static getFacilitiesByReportId<T extends typeof Resource>(this: T) {
        const endpoint = this.list()
        return endpoint.extend({
            url({ reportId }: { reportId: string }) { return `${AuthResource.getBaseUrl()}/api/history/report/${reportId}/facilities` },
            fetch({ reportId }: { reportId: string }) { return endpoint.fetch.call(endpoint, { reportId }) },
            schema: [FacilityResource]
        });
    }
    
}

// const getFacilitiesByReportId = (reportId: string): Promise<FacilityResource> | string => {
//     fetch(`${PATH}`).then(res => {
//             return res.json
//         }
//     );
//     return "Error string"
// }

// const facilitiesListByReport = new Endpoint(getFacilitiesByReportId)