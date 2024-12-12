import { GridContainer, Label, Modal, ModalRef, Select, SiteAlert, TextInput, Tooltip } from "@trussworks/react-uswds";

import { endOfDay, startOfDay, subDays } from "date-fns";
import { useCallback, useMemo, useRef, useState } from "react";
import { Helmet } from "react-helmet-async";
import HipaaNotice from "../../../../components/HipaaNotice";
import useReceiversConnectionStatus from "../../../../hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus";
import { DAY_BACK_DEFAULT } from "../../../../utils/DateTimeUtils";
import DateRangePickerAtomic from "../DateRangePickerAtomic/DateRangePickerAtomic";
import { ReceiversStatusesDisplay } from "../ReceiversStatusesDisplay/ReceiversStatusesDisplay";
import TimePeriodModalInner from "../TimePeriodModal/TimePeriodModal";
import {
    createStatusTimePeriodData,
    filterStatuses,
    RSReceiverStatusParsed,
    SuccessRate,
    TimePeriodData,
} from "../utils";

function AdminReceiverDashboardPage() {
    const defaultDatesRef = useRef({
        start: startOfDay(subDays(new Date(), DAY_BACK_DEFAULT)),
        end: endOfDay(new Date()),
    });

    const [startDate, setStartDate] = useState<Date>(defaultDatesRef.current.start);
    const [endDate, setEndDate] = useState<Date>(defaultDatesRef.current.end);
    // this is the text input box filter
    const [filterReceiver, setFilterReceiver] = useState("");
    const [filterResultMessage, setFilterResultMessage] = useState("");
    const [filterSuccessState, setFilterSuccessState] = useState<SuccessRate>(SuccessRate.UNDEFINED);

    // used to show hide the modal
    const modalShowInfoRef = useRef<ModalRef>(null);
    const [timePeriodStatuses, setTimePeriodStatuses] = useState<RSReceiverStatusParsed[]>([]);

    const handleTimePeriodClick = useCallback(({ entries }: TimePeriodData) => {
        setTimePeriodStatuses(entries);
        modalShowInfoRef?.current?.toggleModal(undefined, true);
    }, []);

    const timePeriodMinutes = 2 * 60;
    const { data: results } = useReceiversConnectionStatus({
        startDate: startDate,
        endDate: endDate,
    });
    const data = useMemo(
        () =>
            createStatusTimePeriodData({
                data: results,
                range: [startDate, endDate],
                filterResultMessage,
                timePeriodMinutes,
            }),
        [endDate, filterResultMessage, results, startDate, timePeriodMinutes],
    );

    const filteredData = filterStatuses(data, filterReceiver, filterSuccessState);

    return (
        <GridContainer>
            <Helmet>
                <title>Receiver status dashboard - Admin</title>
                <meta property="og:image" content="/assets/img/opengraph/reportstream.png" />
                <meta
                    property="og:image:alt"
                    content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                />
            </Helmet>
            <article>
                <h4>Receiver Status Dashboard</h4>
                <section>
                    CRON job results that check if receivers are working.
                    <br />
                    Each slot is a 2hr time slice. Colored slots had a check run. Clicking on a slot shows details.
                </section>
                <SiteAlert variant="info">
                    {
                        "Times shown are in YOUR LOCAL timezone. Be aware that receivers and servers may be in different zones."
                    }
                </SiteAlert>
                <form autoComplete="off" className="grid-row margin-0" name="filter">
                    <div className="flex-auto margin-1">
                        <Label className="font-sans-xs usa-label text-no-wrap" htmlFor="input_filter_receivers">
                            Date range:
                        </Label>
                        <DateRangePickerAtomic
                            defaultStartDate={defaultDatesRef.current.start}
                            defaultEndDate={defaultDatesRef.current.end}
                            onChange={(params) => {
                                setStartDate(params.startDate);
                                setEndDate(params.endDate);
                            }}
                        />
                    </div>
                    <div className="flex-fill margin-1">
                        <Label className="font-sans-xs usa-label text-no-wrap" htmlFor="input_filter_receivers">
                            Receiver Name:
                        </Label>
                        <Tooltip
                            className="fixed-tooltip"
                            label="Filter rows on just the first column's Organization name and Receiver setting name."
                        >
                            <TextInput
                                id="input_filter_receivers"
                                name="input_filter_receivers"
                                type="text"
                                autoComplete="off"
                                aria-autocomplete="none"
                                onChange={(evt) => setFilterReceiver(evt.target.value)}
                            />
                        </Tooltip>
                    </div>

                    <div className="flex-fill margin-1">
                        <Label className="font-sans-xs usa-label text-no-wrap" htmlFor="input_filter_errors">
                            Results Message:
                        </Label>
                        <Tooltip
                            className="fixed-tooltip"
                            label="Filter rows on the Result Message details. This value is found in the details."
                        >
                            <TextInput
                                id="input_filter_errors"
                                name="input_filter_errors"
                                type="text"
                                autoComplete="off"
                                aria-autocomplete="none"
                                onChange={(evt) => setFilterResultMessage(evt.target.value)}
                            />
                        </Tooltip>
                    </div>

                    <div className="flex-fill margin-1">
                        <Label className="font-sans-xs usa-label text-no-wrap" htmlFor="successrate-dropdown">
                            Success Type:
                        </Label>
                        <Tooltip className="fixed-tooltip" label="Show only rows in one of these states.">
                            <Select
                                id="successrate-dropdown"
                                name="successrate-dropdown"
                                onChange={(evt) =>
                                    setFilterSuccessState((evt?.target?.value as SuccessRate) || SuccessRate.UNDEFINED)
                                }
                            >
                                <option value={SuccessRate.UNDEFINED}>Show All</option>
                                <option value={SuccessRate.ALL_FAILURE}>Failed</option>
                                <option value={SuccessRate.MIXED_SUCCESS}>Mixed success</option>
                            </Select>
                        </Tooltip>
                    </div>
                </form>
                {filteredData.length > 0 && (
                    <ReceiversStatusesDisplay
                        receiverStatuses={filteredData}
                        onTimePeriodClick={handleTimePeriodClick}
                    />
                )}
                <Modal isLarge={true} className="rs-admindash-modal" ref={modalShowInfoRef} id={"showSuccessDetails"}>
                    <TimePeriodModalInner receiverStatuses={timePeriodStatuses} />
                </Modal>
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default AdminReceiverDashboardPage;
