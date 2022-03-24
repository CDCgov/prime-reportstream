import { useResource } from "rest-hooks";

import { getStoredOrg } from "../contexts/SessionStorageTools";
import OrganizationResource from "../resources/OrganizationResource";
import { PERMISSIONS } from "../resources/PermissionsResource";

export const getStates = () => {
    return [
        "Alabama",
        "Alaska",
        "American Samoa",
        "Arizona",
        "Arkansas",
        "California",
        "Colorado",
        "Connecticut",
        "Delaware",
        "District of Columbia",
        "Federated States of Micronesia",
        "Florida",
        "Georgia",
        "Guam",
        "Hawaii",
        "Idaho",
        "Illinois",
        "Indiana",
        "Iowa",
        "Kansas",
        "Kentucky",
        "Louisiana",
        "Maine",
        "Marshall Islands",
        "Maryland",
        "Massachusetts",
        "Michigan",
        "Minnesota",
        "Mississippi",
        "Missouri",
        "Montana",
        "Nebraska",
        "Nevada",
        "New Hampshire",
        "New Jersey",
        "New Mexico",
        "New York",
        "North Carolina",
        "North Dakota",
        "Northern Mariana Islands",
        "Ohio",
        "Oklahoma",
        "Oregon",
        "Palau",
        "Pennsylvania",
        "Puerto Rico",
        "Rhode Island",
        "South Carolina",
        "South Dakota",
        "Tennessee",
        "Texas",
        "Utah",
        "Vermont",
        "Virgin Island",
        "Virginia",
        "Washington",
        "West Virginia",
        "Wisconsin",
        "Wyoming",
    ].sort();
};

export const groupToOrg = (group: string | undefined): string => {
    const senderPrefix = `${PERMISSIONS.SENDER}_`;
    const isStandardGroup = group?.startsWith("DH");
    const isSenderGroup = group?.startsWith(senderPrefix);
    /* If isStandardGroup, either sender/receiver, else non-standard */
    const groupType = isStandardGroup
        ? isSenderGroup
            ? "sender"
            : "receiver"
        : "non-standard";

    switch (groupType) {
        case "sender":
            return group ? group.replace(senderPrefix, "") : "";
        case "receiver":
            return group ? group.replace("DH", "").replace("_", "-") : "";
        case "non-standard":
            return group ? group : "";
    }
};

export function useOrgName(): string {
    const org = useResource(OrganizationResource.detail(), {
        name: groupToOrg(getStoredOrg()),
    });
    return org?.description || "";
}
