import { ObjectTooltip } from "../../../components/tooltips/ObjectTooltip";
import { SampleKeysObj } from "../../../utils/TemporarySettingsAPITypes";
import {
    CheckboxComponent,
    DropdownComponent,
    TextAreaComponent,
    TextInputComponent,
} from "../SettingFormFields/SettingFormFields";
import { useSettingForm } from "../SettingFormContext";
import { RSSender } from "../../../config/endpoints/settings";

export interface SenderFieldsetBaseProps {
    defaultValues: Partial<RSSender>;
    mode: "edit" | "clone";
    onChange: <K extends keyof RSSender>(k: K, v: RSSender[K]) => void;
    onJsonInvalid: (e: Error) => void;
}

export function SenderFieldsetBase({
    defaultValues,
    onChange,
    onJsonInvalid,
}: SenderFieldsetBaseProps) {
    return (
        <>
            <DropdownComponent
                fieldname={"format"}
                label={"Format"}
                defaultvalue={defaultValues.format}
                savefunc={(v) => onChange("format", v)}
                valuesFrom={"format"}
            />
            <DropdownComponent
                fieldname={"customerStatus"}
                label={"Customer Status"}
                defaultvalue={defaultValues.customerStatus}
                savefunc={(v) => onChange("customerStatus", v as any)}
                valuesFrom={"customerStatus"}
            />
            <TextInputComponent
                fieldname={"schemaName"}
                label={"Schema Name"}
                defaultvalue={defaultValues.schemaName ?? null}
                savefunc={(v) => onChange("schemaName", v)}
            />
            <TextAreaComponent
                fieldname={"keys"}
                label={"Keys"}
                toolTip={<ObjectTooltip obj={new SampleKeysObj()} />}
                defaultvalue={defaultValues.keys ?? []}
                defaultnullvalue={""}
                savefunc={(v) => onChange("keys", v as any)}
                onJsonInvalid={onJsonInvalid}
            />
            <DropdownComponent
                fieldname={"processingType"}
                label={"Processing Type"}
                defaultvalue={defaultValues.processingType}
                savefunc={(v) => onChange("processingType", v)}
                valuesFrom={"processingType"}
            />
            <CheckboxComponent
                fieldname="allowDuplicates"
                label="Allow Duplicates"
                defaultvalue={defaultValues.allowDuplicates ?? false}
                savefunc={(v) => onChange("allowDuplicates", v)}
            />
        </>
    );
}

export default function SenderFieldset() {
    const { mode, defaultValues, updateSettingProperty, onJsonInvalid } =
        useSettingForm<RSSender>();
    return (
        <SenderFieldsetBase
            defaultValues={defaultValues}
            mode={mode}
            onChange={updateSettingProperty}
            onJsonInvalid={onJsonInvalid}
        />
    );
}
