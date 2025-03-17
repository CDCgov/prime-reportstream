import { useMutation } from "@tanstack/react-query";

export interface CodeMapData {
    "test code": string;
    "test description": string;
    "coding system": string;
    mapped: string;
}

export const sampArr = [
    {
        "test code": "97097-0",
        "test description": "SARS-CoV-2 (COVID-19) Ag [Presence] in Upper respiratory specimen by Rapid  immunoassay",
        "coding system": "LOINC",
        mapped: "Y",
    },
    {
        "test code": "80382-5",
        "test description": "Influenza virus A Ag [Presence] in Upper respiratory specimen by Rapid immunoassay",
        "coding system": "LOINC",
        mapped: "Y",
    },
    {
        "test code": "12345",
        "test description": "Flu B",
        "coding system": "LOCAL",
        mapped: "N",
    },
];

const useCodeMappingFormSubmit = () => {
    const fn = async () => {
        // Simulate network request
        await new Promise((resolve) => setTimeout(resolve, 2500));

        // Return sample JSON
        return sampArr;
    };

    return useMutation({ mutationFn: fn });
};

export default useCodeMappingFormSubmit;
