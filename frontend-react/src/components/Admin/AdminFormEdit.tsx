import {
    Checkbox,
    Grid,
    Label,
    Textarea,
    TextInput,
} from "@trussworks/react-uswds";

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
}): JSX.Element => {
    let defaultValue = JSON.stringify(params?.defaultvalue, undefined, 2);
    if (defaultValue === "null" || defaultValue === "[]") {
        defaultValue = "";
    }

    const key = params.fieldname;
    return (
        <Grid row>
            <Grid col={3}>
                <Label htmlFor={params.fieldname}>{params.label}:</Label>
            </Grid>
            <Grid col={9}>
                <Textarea
                    id={key}
                    name={key}
                    defaultValue={defaultValue}
                    data-testid={key}
                    onBlur={(e) =>
                        params.savefunc(JSON.parse(e?.target?.value || "{}"))
                    }
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
