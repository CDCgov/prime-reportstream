export enum AggregatorType {
    FACILITY = "facility",
    PROVIDER = "provider",
    SUBMITTER = "submitter",
}

export const facilityTypeDisplay = {
    [AggregatorType.FACILITY]: {
        label: "PERFORMING FACILITY",
        className: "bg-mint-cool-5v",
    },
    [AggregatorType.PROVIDER]: {
        label: "ORDERING PROVIDER",
        className: "bg-indigo-cool-10v",
    },
    [AggregatorType.SUBMITTER]: {
        label: "SUBMITTER",
        className: "bg-magenta-10v",
    },
};

export function transformFacilityType(facility: AggregatorType) {
    return facilityTypeDisplay[facility]
        ? facilityTypeDisplay[facility].label
        : facility;
}
