import React from "react";
import { Grid } from "@trussworks/react-uswds";
import classnames from "classnames";

import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import {
    facilityTypeDisplay,
    AggregatorType,
} from "../../../utils/DataDashboardUtils";

import styles from "./FacilityProviderSubmitterSummary.module.scss";

interface SummaryProps {
    details: {
        aggregatorTypeName: string;
        contactName: string;
        location: string;
        phone: string;
        reportDate: string;
        averageTestPerReport: number;
        totalTests: number;
        submitter: string;
        clia: string;
    };
    summaryDetailType: AggregatorType;
}

export function FacilityProviderSubmitterSummary({
    details,
    summaryDetailType,
}: SummaryProps) {
    return (
        <div className={styles.FacilityProviderSubmitterSummary}>
            <div className="margin-bottom-3">
                <h1 className="margin-top-0">{details.aggregatorTypeName}</h1>
                <span
                    className={classnames(
                        "font-mono-3xs border-1px radius-md padding-05 height-3",
                        facilityTypeDisplay[summaryDetailType].className
                    )}
                >
                    {facilityTypeDisplay[summaryDetailType].label}
                </span>
            </div>
            <section className="margin-bottom-4">
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Started reporting to you
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {formatDateWithoutSeconds(details!.reportDate)}
                        </span>
                    </Grid>
                    <Grid col={1}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">CLIA</span>
                    </Grid>
                    <Grid col={5}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">{details?.clia}</span>
                    </Grid>
                </Grid>
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Average tests per report
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {details!.averageTestPerReport}
                        </span>
                    </Grid>
                    <Grid col={1}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Location
                        </span>
                    </Grid>
                    <Grid col={5}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {details?.location}
                        </span>
                    </Grid>
                </Grid>
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Total tests (all time)
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {details!.totalTests}
                        </span>
                    </Grid>
                    <Grid col={1}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Contact
                        </span>
                    </Grid>
                    <Grid col={5}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {details!.contactName}
                        </span>
                    </Grid>
                </Grid>
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Submitter
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {details!.submitter}
                        </span>
                    </Grid>
                    <Grid col={1}>
                        <div className="margin-top-4 font-code-xs"></div>
                    </Grid>
                    <Grid col={5}>
                        <div className="margin-top-4 font-code-xs">
                            {details!.phone}
                        </div>
                    </Grid>
                </Grid>
            </section>
        </div>
    );
}
