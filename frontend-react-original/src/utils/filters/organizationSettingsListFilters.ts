import { RSOrganizationSettings } from "../../config/endpoints/settings";

export function searchOrganizationSettingsList(
    org: RSOrganizationSettings,
    search: string | null,
): boolean {
    if (!search) {
        // no search returns EVERYTHING
        return true;
    }
    // combine all elements to be searched.
    const fullstr =
        `${org.name} ${org.description} ${org.jurisdiction} ${org.stateCode} ${org.countyName}`.toLowerCase();
    return fullstr.includes(`${search.toLowerCase()}`);
}
