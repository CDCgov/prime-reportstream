import React from "react";
import { Grid } from "@trussworks/react-uswds";

import { RSDelivery } from "../../../config/endpoints/dataDashboard";
import ReportLink from "../../../pages/deliveries/Table/ReportLink";
import {
    formatDateWithoutSeconds,
    isDateExpired,
} from "../../../utils/DateTimeUtils";

import styles from "./ReportDetailsSummary.module.scss";

interface Props {
    report: RSDelivery | undefined;
}

export function ReportDetailsSummary(props: Props) {
    const { report }: Props = props;

    return (
        <div className={styles.ReportDetailsSummary}>
            <div className="margin-bottom-3">
                <h1 className="margin-top-0">Report Details</h1>
                {!isDateExpired(report!.expires) && (
                    <div className="font-sans-lg display-inline-flex">
                        Download as <ReportLink report={report} button />
                    </div>
                )}
            </div>
            <section className="margin-bottom-4">
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Report ID
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">{report?.reportId}</span>
                    </Grid>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            File name
                        </span>
                    </Grid>
                    <Grid col={4}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">{report?.fileName}</span>
                    </Grid>
                </Grid>
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Date range
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {formatDateWithoutSeconds(report!.batchReadyAt)} -{" "}
                            {formatDateWithoutSeconds(report!.expires)}
                        </span>
                    </Grid>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Delivery method
                        </span>
                    </Grid>
                    <Grid col={4}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">SFTP</span>
                    </Grid>
                </Grid>
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Date sent to you
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {formatDateWithoutSeconds(report!.batchReadyAt)}{" "}
                        </span>
                    </Grid>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Folder location
                        </span>
                    </Grid>
                    <Grid col={4}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs"></span>
                    </Grid>
                </Grid>
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Available until
                        </span>
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {formatDateWithoutSeconds(report!.expires)}{" "}
                        </span>
                    </Grid>
                </Grid>
            </section>
        </div>
    );
}
