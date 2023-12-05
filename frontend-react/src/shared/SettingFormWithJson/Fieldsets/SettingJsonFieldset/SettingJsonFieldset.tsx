import { Textarea } from "@trussworks/react-uswds";

import {
    SettingFormField,
    SettingFormFieldRow,
} from "../../SettingFormField/SettingFormField";

export default function SettingJsonFieldset() {
    return (
        <SettingFormField
            name="json"
            onChange={(e) => e.currentTarget.value}
            jsonType="whole"
            render={({
                defaultValue,
                id,
                name,
                onChange,
                className,
                disabled,
            }) => (
                <SettingFormFieldRow>
                    <Textarea
                        id={id}
                        name={name}
                        defaultValue={defaultValue}
                        onBlur={onChange}
                        disabled={disabled}
                        className={className}
                        rows={100}
                    />
                </SettingFormFieldRow>
            )}
        />
    );
}
