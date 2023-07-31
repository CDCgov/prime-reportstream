import React from "react";
import { Grid, Icon } from "@trussworks/react-uswds";

import { RSDelivery } from "../../../config/endpoints/deliveries";
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
            <div className="margin-top-1 margin-bottom-3">
                <div className="font-sans-2xl text-bold">Report Details</div>
                {!isDateExpired(report!.expires) && (
                    <div className="font-sans-lg display-flex flex-align-end">
                        Download as <ReportLink report={report} button />
                    </div>
                )}
            </div>
            <section className="margin-bottom-4">
                <Grid row>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="text-bold padding-right-3">
                            Report ID <Icon.Info />
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
                            Available until
                        </span>
                        <hr className="margin-top-2 margin-bottom-2" />
                    </Grid>
                    <Grid col={4} className="padding-right-6">
                        <hr className="margin-top-2 margin-bottom-2" />
                        <span className="font-code-xs">
                            {formatDateWithoutSeconds(report!.expires)}{" "}
                        </span>
                        <hr className="margin-top-2 margin-bottom-2" />
                    </Grid>
                    <Grid col={2}>
                        <hr className="margin-top-2 margin-bottom-2" />
                    </Grid>
                    <Grid col={4}>
                        <hr className="margin-top-2 margin-bottom-2" />
                    </Grid>
                </Grid>
            </section>
        </div>
    );
}
