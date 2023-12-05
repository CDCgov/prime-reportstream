import { ChangeEvent, PropsWithChildren } from "react";
import { Checkbox, Label, TextInput, Textarea } from "@trussworks/react-uswds";

import {
    ObjectTooltip,
    EnumTooltip,
} from "../../../../components/tooltips/ObjectTooltip";
import {
    SampleTranslationObj,
    SampleTimingObj,
    SampleTransportObject,
    reportStreamFilterDefinitionChoices,
} from "../../../../utils/TemporarySettingsAPITypes";
import {
    SettingFormField,
    SettingFormFieldRow,
} from "../../SettingFormField/SettingFormField";

export interface ReceiverFieldsetProps extends PropsWithChildren {}

export default function ReceiverFieldset({ children }: ReceiverFieldsetProps) {
    return (
        <>
            <SettingFormField
                name="translation"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                jsonType="field"
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>Translation</Label>
                                <ObjectTooltip
                                    obj={new SampleTranslationObj()}
                                />
                            </>
                        }
                    >
                        <Textarea
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="jurisdictionalFilter"
                jsonType="field"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>
                                    Jurisdictional Filter
                                </Label>
                                <EnumTooltip
                                    vals={reportStreamFilterDefinitionChoices}
                                />
                            </>
                        }
                    >
                        <Textarea
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="qualityFilter"
                jsonType="field"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>Quality Filter</Label>
                                <EnumTooltip
                                    vals={reportStreamFilterDefinitionChoices}
                                />
                            </>
                        }
                    >
                        <Textarea
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="reverseTheQualityFilter"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.checked
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow>
                        <Checkbox
                            label={"Reverse the Quality Filter"}
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="routingFilter"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>Routing Filter</Label>
                                <EnumTooltip
                                    vals={reportStreamFilterDefinitionChoices}
                                />
                            </>
                        }
                    >
                        <TextInput
                            name={name}
                            type="text"
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="processingModeFilter"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>
                                    Processing Mode Filter
                                </Label>
                                <EnumTooltip
                                    vals={reportStreamFilterDefinitionChoices}
                                />
                            </>
                        }
                    >
                        <TextInput
                            name={name}
                            type="text"
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="deidentify"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.checked
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow>
                        <Checkbox
                            label={"De-identify"}
                            name={name}
                            id={id}
                            defaultChecked={!!defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="timing"
                jsonType="field"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>Timing</Label>
                                <ObjectTooltip obj={new SampleTimingObj()} />
                            </>
                        }
                    >
                        <Textarea
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="transport"
                jsonType="field"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>Transport</Label>
                                <ObjectTooltip
                                    obj={new SampleTransportObject()}
                                />
                            </>
                        }
                    >
                        <Textarea
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="externalName"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={id}>External Name</Label>}
                    >
                        <TextInput
                            name={name}
                            type="text"
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            {children}
        </>
    );
}
