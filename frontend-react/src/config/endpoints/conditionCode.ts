import { HTTPMethods, type RSApiEndpoints, RSEndpoint } from ".";

export enum ConditionCodeUrls {
    CONDITION_COMPARISON = "/sender/conditionCode/comparison",
}

export interface ConditionCodeData {
    testCode: string;
    testDescription: string;
    codingSystem: "LOINC" | "SNOMED" | "LOCAL";
    mapped: string;
}

export const conditionCodeEndpoints: RSApiEndpoints = {
    mapSenderCode: new RSEndpoint({
        path: ConditionCodeUrls.CONDITION_COMPARISON,
        method: HTTPMethods.POST,
        queryKey: "mapSenderCodeResult",
    }),
};
