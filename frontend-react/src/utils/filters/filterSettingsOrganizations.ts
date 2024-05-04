import { Organization } from "../Api";

/**
 * Used by filter edit box ui to show only matched elements.
 * Allows some data to be excluded or cleaned up
 */
function filterSettingsOrganizations(org: Organization, search: string | null): boolean {
    if (!search) {
        // no search returns EVERYTHING
        return true;
    }
    // combine all elements to be searched.
    const fullstr =
        `${org.name} ${org.description} ${org.jurisdiction} ${org.stateCode} ${org.countyName}`.toLowerCase();
    return fullstr.includes(`${search.toLowerCase()}`);
}

export default filterSettingsOrganizations