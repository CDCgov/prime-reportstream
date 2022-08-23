import { Api } from "./Api";

// the shape used by the frontend client for value sets
export interface ValueSet {
    name: string;
    createdBy: string;
    createdAt: string;
    system: string;
}

// the shape sent down by the API for value sets
export interface ApiValueSet {
    name: string;
    created_by: string; // unused
    created_at: string; // unused
    system: string;
    reference: string; // unused
    referenceURL: string; // unused
}

export interface ValueSetRow {
    name: string;
    code: string;
    display: string;
    version: string;
}

export interface LookupTable {
    lookupTableVersionId: number;
    tableName: string;
    tableVersion: number;
    isActive: boolean;
    createdBy: string;
    createdAt: string;
    tableSha256Checksum: string;
}

export enum LookupTables {
    VALUE_SET = "sender_automation_value_set",
    VALUE_SET_ROW = "sender_automation_value_set_row",
}

class LookupTableApi extends Api {
    getTableList = () => {
        return this.configure<LookupTable[]>({
            method: "GET",
            url: `${this.basePath}/listsss`,
            params: { showInactive: true },
        });
    };

    getTableData = <T>(version: number, tableName: string) => {
        return this.configure<T>({
            method: "GET",
            url: `${this.basePath}/${tableName}/${version}/content`,
        });
    };

    saveTableData = <T>(tableName: string) => {
        return this.configure<T>({
            method: "POST",
            url: `${this.basePath}/${tableName}`,
        });
    };

    activateTableData = <T>(version: number, tableName: string) => {
        return this.configure<T>({
            method: "PUT",
            url: `${this.basePath}/${tableName}/${version}/activate`,
        });
    };
}

export const lookupTableApi = new LookupTableApi("lookuptables");
