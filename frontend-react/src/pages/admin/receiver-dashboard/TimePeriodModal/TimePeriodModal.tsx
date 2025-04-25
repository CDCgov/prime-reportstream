import { Grid, GridContainer } from "@trussworks/react-uswds";
import { durationFormatShort } from "../../../../utils/DateTimeUtils";
import { formatDate } from "../../../../utils/misc";
import { RSReceiverStatusParsed } from "../utils";

export interface TimePeriodModalInnerProps {
    receiverStatuses?: RSReceiverStatusParsed[];
}

function TimePeriodModalInner({ receiverStatuses }: TimePeriodModalInnerProps) {
    if (!receiverStatuses || receiverStatuses.length == 0) {
        return <>No Data Found</>;
    }

    const duration = (dataItem: RSReceiverStatusParsed) => {
        return durationFormatShort(
            new Date(dataItem.connectionCheckCompletedAt),
            new Date(dataItem.connectionCheckStartedAt),
        );
    };

    return (
        <GridContainer className={"rs-admindash-modal-container"}>
            {/* We support multiple results per slice */}
            {receiverStatuses.map((dataItem) => (
                <Grid key={`dlog-item-${dataItem.receiverConnectionCheckResultId}`}>
                    <Grid className={"modal-info-title"}>Results for connection verification check</Grid>
                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label"}>Org:</Grid>
                        <Grid className={"modal-info-value"}>
                            {dataItem.organizationName} (id: {dataItem.organizationId})
                        </Grid>
                    </Grid>

                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label "}>Receiver:</Grid>
                        <Grid className={"modal-info-value"}>
                            {dataItem.receiverName} (id: {dataItem.receiverId})
                        </Grid>
                    </Grid>

                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label"}>Result:</Grid>
                        <Grid
                            className={`modal-info-value ${
                                dataItem.connectionCheckSuccessful ? "success-all" : "failure-all"
                            }`}
                        >
                            {dataItem.connectionCheckSuccessful ? "success" : "failed"}
                        </Grid>
                    </Grid>

                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label"}>Started At:</Grid>
                        <Grid className={"modal-info-value"}>
                            {formatDate(dataItem.connectionCheckStartedAt)}
                            <br />
                            {dataItem.connectionCheckStartedAt.toISOString()}
                        </Grid>
                    </Grid>

                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label"}>Time to complete:</Grid>
                        <Grid className={"modal-info-value"}>
                            {duration(dataItem)}
                            <br />
                        </Grid>
                    </Grid>

                    <Grid row className={"modal-info-row"}>
                        <Grid className={"modal-info-label"}>Result message:</Grid>
                        <Grid className={"modal-info-value"}>{dataItem.connectionCheckResult}</Grid>
                    </Grid>
                </Grid>
            ))}
        </GridContainer>
    );
}

export default TimePeriodModalInner;
