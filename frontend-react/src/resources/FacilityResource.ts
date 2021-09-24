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

    static urlRoot = `${AuthResource.getBaseUrl()}/api/history/report/{reportId}/facilities`;

    static getFacilities<FacilityResource>(this: FacilityResource, reportId: string | undefined) {
        const endpoint = super.list();
        return endpoint.extend({
          fetch() { return endpoint(this); },
          url() { return `/api/history/report/${reportId}/facilities` },
        });
    };
}