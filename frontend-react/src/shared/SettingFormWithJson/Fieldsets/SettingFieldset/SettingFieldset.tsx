import { Label, TextInput } from "@trussworks/react-uswds";
import { ChangeEvent } from "react";

import {
    SettingFormField,
    SettingFormFieldRow,
} from "../../SettingFormField/SettingFormField";

export default function SettingFieldset() {
    return (
        <>
            <SettingFormField
                name="name"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    mode,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={id} requiredMarker>
                                Name
                            </Label>
                        }
                    >
                        <TextInput
                            name={name}
                            type="text"
                            id={id}
                            defaultValue={defaultValue}
                            disabled={disabled && mode === "edit"}
                            onBlur={onChange}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="description"
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
                        label={<Label htmlFor={id}>Description</Label>}
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
        </>
    );
}
