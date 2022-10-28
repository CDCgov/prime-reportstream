import React from "react";

import { StaticAlert } from "../StaticAlert";
import { capitalizeFirst } from "../../utils/misc";

interface NoServicesBannerProps {
    featureName?: string;
    organization?: string;
    serviceType?: "sender" | "receiver";
}

export const NoServicesBanner = ({
    featureName,
    organization,
    serviceType,
}: NoServicesBannerProps) => {
    return (
        <StaticAlert
            type={"error"}
            heading={`${capitalizeFirst(featureName || "feature")} unavailable`}
            message={`No valid ${serviceType || "service"} found for ${
                organization || "your organization"
            }`}
        />
    );
};
