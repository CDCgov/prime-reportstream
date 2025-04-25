import { Button, DateRangePicker, Modal, ModalRef, ModalToggleButton } from "@trussworks/react-uswds";
import { differenceInDays, endOfDay } from "date-fns";
import { useCallback, useRef, useState } from "react";
import { StyleClass, TableFilterDateLabel } from "../../../../components/Table/TableFilters";
import { MAX_DAYS_MS } from "../constants";

export interface DateRangePickerAtomicProps {
    defaultStartDate: Date;
    defaultEndDate: Date;
    onChange: (props: { startDate: Date; endDate: Date }) => void;
}

/**
 * We want to control date picking better than the default control allows.
 * Reasons:
 *  - We limit the max range allowed to limit large data requests.
 *  - We want start AND end picked before the expensive fetch.
 *  - Picker fields are LARGE and take up a bunch of space.
 */
function DateRangePickerAtomic({ defaultEndDate, defaultStartDate, onChange }: DateRangePickerAtomicProps) {
    const [startDate, setStartDate] = useState(defaultStartDate);
    const [endDate, setEndDate] = useState(defaultEndDate);
    const modalRef = useRef<ModalRef>(null);

    // for readability
    const isDateRangeOk = useCallback(() => {
        return differenceInDays(endDate, startDate) < MAX_DAYS_MS;
    }, [startDate, endDate]);

    return (
        <>
            <span>
                🗓 {startDate.toLocaleDateString()}
                {" — "}
                {endDate.toLocaleDateString()}
            </span>
            <ModalToggleButton modalRef={modalRef} opener className="padding-1 margin-1 usa-button--outline">
                Change...
            </ModalToggleButton>
            <Modal ref={modalRef} id={"date-range-picker"}>
                <div>Select date range to show. (Max 10 days span)</div>
                <DateRangePicker
                    className={`${StyleClass.DATE_CONTAINER} margin-bottom-5`}
                    startDateLabel={TableFilterDateLabel.START_DATE}
                    startDatePickerProps={{
                        id: "start-date",
                        name: "start-date-picker",
                        defaultValue: defaultStartDate.toLocaleDateString(),
                        onChange: (s) => {
                            if (s) {
                                setStartDate(new Date(s));
                            }
                        },
                    }}
                    endDateLabel={TableFilterDateLabel.END_DATE}
                    endDatePickerProps={{
                        id: "end-date",
                        name: "end-date-picker",
                        defaultValue: defaultEndDate.toLocaleDateString(),
                        onChange: (s) => {
                            if (s) {
                                setEndDate(endOfDay(new Date(s)));
                            }
                        },
                    }}
                />
                <Button
                    type="button"
                    disabled={!isDateRangeOk()}
                    onClick={() => {
                        modalRef.current?.toggleModal(undefined, false);
                        onChange({ startDate, endDate });
                    }}
                >
                    Update
                </Button>
                {isDateRangeOk() ? null : (
                    <span className={"rs-admindash-warn-font"}>
                        Dates are too far apart (too many days - max 10 days)
                    </span>
                )}
            </Modal>
        </>
    );
}

export default DateRangePickerAtomic;
