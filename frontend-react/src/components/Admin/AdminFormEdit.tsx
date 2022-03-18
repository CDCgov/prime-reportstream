import {
    Checkbox,
    Grid,
    Label,
    Textarea,
    TextInput,
} from "@trussworks/react-uswds";
import { useRef } from "react";

import { showError } from "../AlertNotifications";

export const TextInputComponent = (params: {
    fieldname: string;
    label: string;
    defaultvalue: string | null;
    savefunc: (val: string) => void;
    disabled?: boolean;
}): JSX.Element => {
    const key = params.fieldname;
    return (
        <Grid row>
            <Grid col={3}>
                <Label htmlFor={params.fieldname}>{params.label}:</Label>
            </Grid>
            <Grid col={9}>
                <TextInput
                    id={key}
                    name={key}
                    type="text"
                    defaultValue={params.defaultvalue || ""}
                    data-testid={key}
                    maxLength={255}
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
            </Grid>
            <Grid col={9}>
                <Textarea
                    inputRef={inputRef}
                    id={key}
                    name={key}
                    defaultValue={defaultValue}
                    data-testid={key}
                    onBlur={(e) => {
                        const inputvalue =
                            e?.target?.value || (defaultnullvalue as string);
                        try {
                            const textJson = JSON.parse(inputvalue);
                            params.savefunc(textJson);
                        } catch (err: any) {
                            // message like `'Unexpected token _ in JSON at position 164'`
                            showError(
                                `Element "${key}" generated an error "${err?.message}"`
                            );
                            // now we parse out the position and try to select it for them.
                            const findPositionMatch = err?.message
                                ?.matchAll(/position (\d+)/gi)
                                ?.next();
                            if (findPositionMatch?.value?.length === 2) {
                                let offset = parseInt(
                                    findPositionMatch.value[1] || -1
                                );
                                if (!isNaN(offset) && offset !== -1) {
                                    if (offset > 4) {
                                        offset -= 4;
                                    }
                                    const end = Math.min(
                                        offset + 8,
                                        inputvalue.length - 1
                                    );
                                    inputRef?.current?.focus();
                                    inputRef?.current?.setSelectionRange(
                                        offset,
                                        end
                                    );
                                }
                            }
                        }
                    }}
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
