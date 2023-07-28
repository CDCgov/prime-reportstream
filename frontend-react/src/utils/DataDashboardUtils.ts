export enum SenderType {
    FACILITY = "facility",
    PROVIDER = "provider",
    SUBMITTER = "submitter",
}

export const facilityTypeDisplay = {
    [SenderType.FACILITY]: {
        label: "PERFORMING FACILITY",
        className:
            "font-mono-3xs radius-md padding-05 height-3 bg-mint-cool-5v",
    },
    [SenderType.PROVIDER]: {
        label: "ORDERING PROVIDER",
        className:
            "font-mono-3xs radius-md padding-05 height-3 bg-indigo-cool-10v",
    },
    [SenderType.SUBMITTER]: {
        label: "SUBMITTER",
        className: "font-mono-3xs radius-md padding-05 height-3 bg-magenta-10v",
    },
};

export function transformFacilityTypeLabel(facility: string) {
    const facilityType = facility.toLowerCase() as SenderType;
    return facilityTypeDisplay[facilityType]
        ? facilityTypeDisplay[facilityType].label
        : facility;
}

export function transformFacilityTypeClass(facility: string) {
    const facilityType = facility.toLowerCase() as SenderType;
    return facilityTypeDisplay[facilityType]
        ? facilityTypeDisplay[facilityType].className
        : "";
}
