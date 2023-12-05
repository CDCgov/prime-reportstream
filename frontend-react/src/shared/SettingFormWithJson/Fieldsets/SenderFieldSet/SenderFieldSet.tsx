import {
    Checkbox,
    Label,
    Select,
    TextInput,
    Textarea,
} from "@trussworks/react-uswds";
import { ChangeEvent, PropsWithChildren } from "react";

import { ObjectTooltip } from "../../../../components/tooltips/ObjectTooltip";
import {
    SampleKeysObj,
    formatChoices,
    processingTypeChoices,
} from "../../../../utils/TemporarySettingsAPITypes";
import {
    SettingFormField,
    SettingFormFieldRow,
} from "../../SettingFormField/SettingFormField";

export interface SenderFieldsetProps extends PropsWithChildren {}

export default function SenderFieldset({ children }: SenderFieldsetProps) {
    return (
        <>
            <SettingFormField
                name="format"
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
                        label={<Label htmlFor={id}>Format</Label>}
                    >
                        <Select
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                            title="Format"
                        >
                            {formatChoices.map((c) => (
                                <option key={c} value={c}>
                                    {c}
                                </option>
                            ))}
                        </Select>
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="schemaName"
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
                        label={<Label htmlFor={id}>Schema Name</Label>}
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
                name="keys"
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
                                <Label htmlFor={id}>Keys</Label>
                                <ObjectTooltip obj={new SampleKeysObj()} />
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
                name="processingType"
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
                        label={<Label htmlFor={id}>Processing Type</Label>}
                    >
                        <Select
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                            title="Processing Type"
                        >
                            {processingTypeChoices.map((c) => (
                                <option key={c} value={c}>
                                    {c}
                                </option>
                            ))}
                        </Select>
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="allowDuplicates"
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
                            label={"Allow Duplicates"}
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
            {children}
        </>
    );
}
