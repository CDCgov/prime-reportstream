// AutoUpdateFileChromatic
import React from "react";

import { Feature } from "./Feature";

export default {
    title: "components/Feature",
    component: Feature,
};

const section = {
    title: "This is a section",
    features: [
        {
            title: "1. Create a personalized plan to meet your unique public health data needs",
            summary:
                "Our expert team works with your preferences to accept data in HL7, FHIR and CSV, and deliver data through multiple connection types.",
        },
        {
            summary:
                "SimpleReport is a free tool for reporting organizations with less technical capacity than ReportStream users. Public health entities receive data from SimpleReport if they are connected with ReportStream.",
            img: "/assets/img/simpleReport.png",
        },
    ],
};

export const RSFeature = (): React.ReactElement => (
    <Feature section={section} feature={section.features[0]} />
);

export const RSFeatureWithImage = (): React.ReactElement => (
    <Feature section={section} feature={section.features[1]} />
);
