import {
    EnumTooltip,
    ObjectTooltip,
} from "../../../components/tooltips/ObjectTooltip";
import {
    getListOfEnumValues,
    SampleTimingObj,
    SampleTranslationObj,
    SampleTransportObject,
} from "../../../utils/TemporarySettingsAPITypes";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useSettingForm } from "../SettingFormContext";
import {
    CheckboxComponent,
    DropdownComponent,
    TextAreaComponent,
    TextInputComponent,
} from "../SettingFormFields/SettingFormFields";

export interface ReceiverFieldSetBaseProps {
    defaultValues: Partial<RSReceiver>;
    mode: "edit" | "clone";
    onChange: (k: keyof RSReceiver, v: RSReceiver[typeof k]) => void;
    onJsonInvalid: (e: Error) => void;
}

export function ReceiverFieldSetBase({
    defaultValues,
    onChange,
    onJsonInvalid,
}: ReceiverFieldSetBaseProps) {
    return (
        <>
            <DropdownComponent
                fieldname={"customerStatus"}
                label={"Customer Status"}
                defaultvalue={defaultValues.customerStatus}
                savefunc={(v) => onChange("customerStatus", v)}
                valuesFrom={"customerStatus"}
            />
            <DropdownComponent
                fieldname={"timeZone"}
                label={"Time Zone"}
                defaultvalue={defaultValues.timeZone}
                savefunc={(v) => onChange("timeZone", v)}
                valuesFrom={"timeZone"}
            />
            <DropdownComponent
                fieldname={"dateTimeFormat"}
                label={"Date Time Format"}
                defaultvalue={defaultValues.dateTimeFormat}
                savefunc={(v) => onChange("dateTimeFormat", v)}
                valuesFrom={"dateTimeFormat"}
            />
            <TextInputComponent
                fieldname={"description"}
                label={"Description"}
                defaultvalue={defaultValues.description ?? null}
                savefunc={(v) => onChange("description", v)}
            />
            <TextAreaComponent
                fieldname={"translation"}
                label={"Translation"}
                toolTip={<ObjectTooltip obj={new SampleTranslationObj()} />}
                defaultvalue={defaultValues.translation}
                defaultnullvalue={null}
                savefunc={(v) => onChange("translation", v)}
                onJsonInvalid={onJsonInvalid}
            />
            <TextAreaComponent
                fieldname={"jurisdictionalFilter"}
                label={"Jurisdictional Filter"}
                toolTip={
                    <EnumTooltip
                        vals={getListOfEnumValues(
                            "reportStreamFilterDefinition",
                        )}
                    />
                }
                defaultvalue={defaultValues.jurisdictionalFilter ?? []}
                defaultnullvalue="[]"
                savefunc={(v) => onChange("jurisdictionalFilter", v)}
                onJsonInvalid={onJsonInvalid}
            />
            <TextAreaComponent
                fieldname={"qualityFilter"}
                label={"Quality Filter"}
                toolTip={
                    <EnumTooltip
                        vals={getListOfEnumValues(
                            "reportStreamFilterDefinition",
                        )}
                    />
                }
                defaultvalue={defaultValues.qualityFilter ?? []}
                defaultnullvalue="[]"
                savefunc={(v) => onChange("qualityFilter", v)}
                onJsonInvalid={onJsonInvalid}
            />
            <CheckboxComponent
                fieldname={"reverseTheQualityFilter"}
                label={"Reverse the Quality Filter"}
                defaultvalue={defaultValues.reverseTheQualityFilter ?? false}
                savefunc={(v) => onChange("reverseTheQualityFilter", v)}
            />
            <TextAreaComponent
                fieldname={"routingFilter"}
                label={"Routing Filter"}
                toolTip={
                    <EnumTooltip
                        vals={getListOfEnumValues(
                            "reportStreamFilterDefinition",
                        )}
                    />
                }
                defaultvalue={defaultValues.routingFilter ?? []}
                defaultnullvalue="[]"
                savefunc={(v) => onChange("routingFilter", v)}
                onJsonInvalid={onJsonInvalid}
            />
            <TextAreaComponent
                fieldname={"processingModeFilter"}
                label={"Processing Mode Filter"}
                toolTip={
                    <EnumTooltip
                        vals={getListOfEnumValues(
                            "reportStreamFilterDefinition",
                        )}
                    />
                }
                defaultvalue={defaultValues.processingModeFilter ?? []}
                defaultnullvalue="[]"
                savefunc={(v) => onChange("processingModeFilter", v)}
                onJsonInvalid={onJsonInvalid}
            />
            <CheckboxComponent
                fieldname={"deidentify"}
                label={"De-identify"}
                defaultvalue={defaultValues.deidentify ?? false}
                savefunc={(v) => onChange("deidentify", v)}
            />
            <TextAreaComponent
                fieldname={"timing"}
                label={"Timing"}
                toolTip={<ObjectTooltip obj={new SampleTimingObj()} />}
                defaultvalue={defaultValues.timing ?? {}}
                defaultnullvalue={null}
                savefunc={(v) => onChange("timing", v)}
                onJsonInvalid={onJsonInvalid}
            />
            <TextAreaComponent
                fieldname={"transport"}
                label={"Transport"}
                toolTip={<ObjectTooltip obj={new SampleTransportObject()} />}
                defaultvalue={defaultValues.transport ?? {}}
                defaultnullvalue={null}
                savefunc={(v) => onChange("transport", v)}
                onJsonInvalid={onJsonInvalid}
            />
            <TextInputComponent
                fieldname={"externalName"}
                label={"External Name"}
                defaultvalue={defaultValues.externalName ?? null}
                savefunc={(v) => onChange("externalName", v)}
            />
        </>
    );
}

export default function ReceiverFieldSet() {
    const { mode, defaultValues, updateSettingProperty, onJsonInvalid } =
        useSettingForm<RSReceiver>();
    return (
        <ReceiverFieldSetBase
            defaultValues={defaultValues}
            mode={mode}
            onChange={updateSettingProperty}
            onJsonInvalid={onJsonInvalid}
        />
    );
}
