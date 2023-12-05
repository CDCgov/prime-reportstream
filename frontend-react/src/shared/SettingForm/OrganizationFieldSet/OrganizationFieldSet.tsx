import { Grid } from "@trussworks/react-uswds";

import {
    TextInputComponent,
    DropdownComponent,
    TextAreaComponent,
} from "../SettingFormFields/SettingFormFields";
import { DisplayMeta } from "../../../components/Admin/DisplayMeta";
import { ObjectTooltip } from "../../../components/tooltips/ObjectTooltip";
import { SampleFilterObject } from "../../../utils/TemporarySettingsAPITypes";
import { RSOrganizationSettings } from "../../../config/endpoints/settings";
import { useSettingForm } from "../SettingFormContext";

export interface OrganizationFieldSetBaseProps {
    defaultValues: Partial<RSOrganizationSettings>;
    mode: "edit" | "clone";
    onChange: <K extends keyof RSOrganizationSettings>(
        k: K,
        v: RSOrganizationSettings[K],
    ) => void;
    onJsonInvalid: (e: Error) => void;
}

export function OrganizationFieldSetBase({
    defaultValues,
    onChange,
    onJsonInvalid,
}: OrganizationFieldSetBaseProps) {
    return (
        <>
            <Grid row>
                <Grid col={3}>Meta:</Grid>
                <Grid col={9}>
                    <DisplayMeta metaObj={defaultValues as any} />
                    <br />
                </Grid>
            </Grid>
            <TextInputComponent
                fieldname={"description"}
                label={"Description"}
                defaultvalue={defaultValues.description ?? null}
                savefunc={(v) => onChange("description", v)}
            />
            <DropdownComponent
                fieldname={"jurisdiction"}
                label={"Jurisdiction"}
                defaultvalue={defaultValues.jurisdiction}
                savefunc={(v) => onChange("jurisdiction", v)}
                valuesFrom={"jurisdiction"}
            />
            <TextInputComponent
                fieldname={"countyName"}
                label={"County Name"}
                defaultvalue={defaultValues.countyName ?? null}
                savefunc={(v) => onChange("countyName", v)}
            />
            <TextInputComponent
                fieldname={"stateCode"}
                label={"State Code"}
                defaultvalue={defaultValues.stateCode ?? null}
                savefunc={(v) => onChange("stateCode", v)}
            />
            <TextAreaComponent
                fieldname={"filters"}
                label={"Filters"}
                toolTip={<ObjectTooltip obj={new SampleFilterObject()} />}
                defaultvalue={defaultValues.filters ?? []}
                defaultnullvalue="[]"
                savefunc={(v) => onChange("filters", v as string[])}
                onJsonInvalid={onJsonInvalid}
            />
        </>
    );
}

export default function OrganizationFieldSet() {
    const { mode, defaultValues, updateSettingProperty, onJsonInvalid } =
        useSettingForm<RSOrganizationSettings>();
    return (
        <OrganizationFieldSetBase
            defaultValues={defaultValues}
            mode={mode}
            onChange={updateSettingProperty}
            onJsonInvalid={onJsonInvalid}
        />
    );
}
