// AutoUpdateFileChromatic
import React from "react";

import { Tile } from "./Tile";

export default {
    title: "components/Tile",
    component: Tile,
};

const section = {
    title: "This is a section",
    items: [
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

export const RSTile = (): React.ReactElement => <Tile {...section.items[0]} />;

export const RSTileWithImage = (): React.ReactElement => (
    <Tile {...section.items[1]} />
);
