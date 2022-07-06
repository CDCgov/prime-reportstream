import { API, Endpoint } from "./NewApi";

export class OrganizationResource {
    readonly name: string = "name";
    readonly description: string = "description";
    readonly jurisdiction: string = "jurisdiction";
    readonly stateCode: string = "state";
    readonly countyName: string = "county";
}

export const OrganizationsAPI: API = {
    resource: OrganizationResource,
    baseUrl: "/api/settings/organizations",
    endpoints: new Map<string, Endpoint>([
        [
            "detail",
            {
                url: "/:org",
                methods: ["GET"],
            },
        ],
    ]),
};
