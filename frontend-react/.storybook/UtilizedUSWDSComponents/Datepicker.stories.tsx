import React from "react";
import { DateRangePicker } from "@trussworks/react-uswds";
import {
    TableFilterDateLabel,
    StyleClass,
} from "../../src/components/Table/TableFilters";

export default {
    title: "Components/Date range picker",
    component: DateRangePicker,
};

type StorybookArguments = {
    onSubmit: React.FormEventHandler<HTMLFormElement>;
    disabled?: boolean;
    validationStatus?: undefined | "error" | "success";
};

export const rangeDatePicker = (
    argTypes: StorybookArguments
): React.ReactElement => {
    const currentDate = new Date();
    const currentDatePlusMonth = currentDate.setMonth(
        currentDate.getMonth() + 1
    );
    return (
        <DateRangePicker
            className={`${StyleClass.DATE_CONTAINER} margin-bottom-5`}
            startDateLabel={TableFilterDateLabel.START_DATE}
            startDatePickerProps={{
                id: "start-date",
                name: "start-date-picker",
                defaultValue: currentDate.toLocaleDateString(),
            }}
            endDateLabel={TableFilterDateLabel.END_DATE}
            endDatePickerProps={{
                id: "end-date",
                name: "end-date-picker",
                defaultValue: new Date(
                    currentDatePlusMonth
                ).toLocaleDateString(),
            }}
        />
    );
};
