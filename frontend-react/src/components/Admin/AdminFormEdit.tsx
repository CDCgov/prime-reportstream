import {
    Grid,
    Checkbox,
    Label,
    Textarea,
    TextInput,
    Dropdown,
} from "@trussworks/react-uswds";
import { PropsWithChildren, useRef } from "react";

import { checkTextAreaJson } from "../../utils/misc";
import {
    getListOfEnumValues,
    ReportStreamSettingsEnum,
} from "../../utils/TemporarySettingsAPITypes";

export const TextInputComponent = (params: {
    fieldname: string;
    label: string;
    defaultvalue: string | null;
    savefunc: (val: string) => void;
    disabled?: boolean;
    toolTip?: JSX.Element;
}): JSX.Element => {
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
                    defaultValue={params.defaultvalue || ""}
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
    if (
        defaultValue === "null" ||
        defaultValue === "[]" ||
        defaultValue === "{}"
    ) {
        defaultValue = "";
    }

    const key = params.fieldname;
    const defaultnullvalue = params.defaultnullvalue
        ? params.defaultnullvalue
        : null;
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
                        const text =
                            e?.target?.value || (defaultnullvalue as string);
                        const result = checkTextAreaJson(text, key, inputRef);
                        if (result !== false) {
                            // checkTextAreaJson made sure the following call won't throw.
                            params.savefunc(result);
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
            <Grid col={9}>
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

export const DropdownComponent = (
    params: PropsWithChildren<{
        fieldname: string;
        label: string;
        defaultvalue: string | undefined;
        savefunc: (val: string) => void;
        disabled?: boolean;
        toolTip?: JSX.Element;
        valuesFrom: ReportStreamSettingsEnum;
    }>
): JSX.Element => {
    const key = params.fieldname;
    return (
        <Grid row>
            <Grid col={3}>
                <Label htmlFor={params.fieldname}>{params.label}:</Label>
                {params.toolTip ? params.toolTip : null}
            </Grid>
            <Grid col={9}>
                <Dropdown
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
                </Dropdown>
            </Grid>
        </Grid>
    );
};
