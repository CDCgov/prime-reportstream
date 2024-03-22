import { Select, Label } from "@trussworks/react-uswds";
import React, { ReactElement } from "react";

export default {
    title: "Components/Select",
    component: "Select",
};

export const defaultDropdown = (): ReactElement => (
    <Select id="input-dropdown" name="input-dropdown">
        <option>- Select - </option>
        <option value="value1">Option A</option>
        <option value="value2">Option B</option>
        <option value="value3">Option C</option>
    </Select>
);

export const withDefaultValue = (): ReactElement => (
    <Select id="input-dropdown" name="input-dropdown" defaultValue="value2">
        <option>- Select - </option>
        <option value="value1">Option A</option>
        <option value="value2">Option B</option>
        <option value="value3">Option C</option>
    </Select>
);

export const withLabel = (): ReactElement => (
    <>
        <Label htmlFor="options">Select label</Label>
        <Select id="input-dropdown" name="input-dropdown">
            <option>- Select - </option>
            <option value="value1">Option A</option>
            <option value="value2">Option B</option>
            <option value="value3">Option C</option>
        </Select>
    </>
);

export const disabled = (): ReactElement => (
    <Select id="input-dropdown" name="input-dropdown" disabled>
        <option>- Select - </option>
        <option value="value1">Option A</option>
        <option value="value2">Option B</option>
        <option value="value3">Option C</option>
    </Select>
);
