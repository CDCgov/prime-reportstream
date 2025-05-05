import { Checkbox, Grid, Label, Select, Textarea, TextInput } from "@trussworks/react-uswds";
import { useRef } from "react";

import { showToast } from "../../contexts/Toast";
import { checkJson } from "../../utils/misc";
import { getListOfEnumValues, ReportStreamSettingsEnum } from "../../utils/TemporarySettingsAPITypes";

export interface TextInputComponentProps {
    fieldname: string;
    label: string;
    defaultvalue: string | null;
    savefunc: (val: string) => void;
    disabled?: boolean;
    toolTip?: JSX.Element;
}

export const TextInputComponent = (params: TextInputComponentProps): JSX.Element => {
    const key = params.fieldname;
    return (
        <Grid row>
            <Grid col={3}>
                <Label htmlFor={params.fieldname}>{params.label}:</Label>
                {params.toolTip ? params.toolTip : null}
            </Grid>
            <Grid col={9}>
                <TextInput
                    id={key}
                    name={key}
                    type="text"
                    defaultValue={params.defaultvalue ?? ""}
                    data-testid={key}
                    maxLength={255}
                    className="rs-input"
                    onChange={(e) => params.savefunc(e?.target?.value || "")}
                    disabled={params.disabled}
                />
            </Grid>
        </Grid>
    );
};

export const TextAreaComponent = (params: {
    fieldname: string;
    label: string;
    defaultvalue: object;
    savefunc: (val: object) => void;
    defaultnullvalue: string | null;
    disabled?: boolean;
    toolTip?: JSX.Element;
}): JSX.Element => {
    const inputRef = useRef<HTMLTextAreaElement>(null);
    let defaultValue = JSON.stringify(params?.defaultvalue, undefined, 2);
    if (defaultValue === "null" || defaultValue === "[]" || defaultValue === "{}") {
        defaultValue = "";
    }

    const key = params.fieldname;
    const defaultnullvalue = params.defaultnullvalue ? params.defaultnullvalue : null;
    return (
        <Grid row>
            <Grid col={3}>
                <Label htmlFor={params.fieldname}>{params.label}:</Label>
                {params.toolTip ? params.toolTip : null}
            </Grid>
            <Grid col={9}>
                <Textarea
                    inputRef={inputRef}
                    id={key}
                    name={key}
                    defaultValue={defaultValue}
                    data-testid={key}
                    className="rs-input"
                    onBlur={(e) => {
                        const text = e?.target?.value || defaultnullvalue!;
                        const { valid, errorMsg } = checkJson(text);
                        if (valid) {
                            // checkJson made sure the following JSON.parse won't throw.
                            params.savefunc(JSON.parse(text));
                        } else {
                            showToast(`JSON data generated an error "${errorMsg}"`, "error");
                        }
                    }}
                    disabled={params.disabled}
                />
            </Grid>
        </Grid>
    );
};

export const CheckboxComponent = (params: {
    fieldname: string;
    label: string;
    defaultvalue: boolean;
    savefunc: (val: boolean) => void;
}): JSX.Element => {
    const key = params.fieldname;
    return (
        <Grid row>
            <Grid col={3}>
                <Label htmlFor={params.fieldname}>{params.label}:</Label>
            </Grid>
            <Grid col={9} className={"padding-top-1"}>
                <Checkbox
                    id={key}
                    name={key}
                    defaultChecked={params.defaultvalue}
                    data-testid={key}
                    label=""
                    onChange={(e) => params.savefunc(e?.target?.checked)}
                />
            </Grid>
        </Grid>
    );
};

export interface DropdownProps {
    fieldname: string;
    label: string;
    defaultvalue: string | undefined;
    savefunc: (val: string) => void;
    disabled?: boolean;
    toolTip?: JSX.Element;
    valuesFrom: ReportStreamSettingsEnum;
}

export const DropdownComponent = (params: DropdownProps): JSX.Element => {
    const key = params.fieldname;
    return (
        <Grid row>
            <Grid col={3}>
                <Label htmlFor={params.fieldname}>{params.label}:</Label>
                {params.toolTip ? params.toolTip : null}
            </Grid>
            <Grid col={9}>
                <Select
                    id={key}
                    data-testid={key}
                    name={key}
                    defaultValue={params.defaultvalue}
                    className="rs-input"
                    onChange={(e) => params.savefunc(e?.target?.value)}
                >
                    <option value={""}>-- Please Select --</option>
                    {getListOfEnumValues(params.valuesFrom).map((v) => (
                        <option key={key + v} value={v}>
                            {v}
                        </option>
                    ))}
                </Select>
            </Grid>
        </Grid>
    );
};
