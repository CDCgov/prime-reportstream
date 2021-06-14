import FacilityResource  from './FacilityResource'
import ActionResource from './ActionResource'
import AuthResource from './AuthResource';

export default class ReportResource extends AuthResource {

    readonly sent: number = 1;
    readonly via: string = "Via";
    readonly positive: number = 1;
    readonly total: number = 1;
    readonly fileType: string = "fileType";
    readonly type: string ="type";
    readonly reportId: string = "reportId";
    readonly expires: number = 1;
    readonly sendingOrg: string = "sendingOrg";
    readonly receivingOrg: string = "receivingOrg";
    readonly facilities: FacilityResource[] = [];
    readonly actions: ActionResource[] = [];

    pk(){
        return this.reportId;
    }

    static urlRoot = 'http://localhost:7071/api/history/report';
}