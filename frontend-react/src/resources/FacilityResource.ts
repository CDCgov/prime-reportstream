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
}