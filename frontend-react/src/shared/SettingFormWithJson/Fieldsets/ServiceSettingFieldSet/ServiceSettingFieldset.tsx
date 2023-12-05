import { ChangeEvent, PropsWithChildren } from "react";
import { Label, Select, TextInput } from "@trussworks/react-uswds";

import {
    SettingFormField,
    SettingFormFieldRow,
} from "../../SettingFormField/SettingFormField";
import { customerStatusChoices } from "../../../../utils/TemporarySettingsAPITypes";

export interface ServiceSettingFieldsetProps extends PropsWithChildren {}

export default function ServiceSettingFieldset({
    children,
}: ServiceSettingFieldsetProps) {
    return (
        <>
            <SettingFormField
                name="topic"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    mode,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={id} requiredMarker>
                                Topic
                            </Label>
                        }
                    >
                        <TextInput
                            name={name}
                            type="text"
                            id={id}
                            defaultValue={defaultValue}
                            disabled={mode === "edit"}
                            onBlur={onChange}
                            required
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="customerStatus"
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
                        label={<Label htmlFor={id}>Customer Status</Label>}
                    >
                        <Select
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                            title="Customer Status"
                        >
                            {customerStatusChoices.map((c) => (
                                <option key={c} value={c}>
                                    {c}
                                </option>
                            ))}
                        </Select>
                    </SettingFormFieldRow>
                )}
            />
            {children}
        </>
    );
}
