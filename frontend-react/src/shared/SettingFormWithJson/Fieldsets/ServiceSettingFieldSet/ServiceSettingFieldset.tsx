import { Label, Select, TextInput } from "@trussworks/react-uswds";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { SettingFormFieldsetProps } from "../SettingFormFieldset";
import {
    RSService,
    customerStatusChoices,
} from "../../../../config/endpoints/settings";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";

export interface ServiceSettingFieldsetProps extends SettingFormFieldsetProps {}

export default function ServiceSettingFieldset({
    children,
}: ServiceSettingFieldsetProps) {
    const { form, mode } = useSettingForm<RSService>();
    return (
        <>
            <form.Field
                name="topic"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name} requiredMarker>
                                Topic
                            </Label>
                        }
                    >
                        <TextInput
                            name={field.name}
                            type="text"
                            id={field.name}
                            value={field.state.value}
                            disabled={mode === "edit" || mode === "readonly"}
                            required
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value)
                            }
                        />
                    </SettingFormFieldRow>
                )}
            />
            <form.Field
                name="customerStatus"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name}>Customer Status</Label>
                        }
                    >
                        <Select
                            name={field.name}
                            id={field.name}
                            value={field.state.value}
                            disabled={mode === "readonly"}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value as any)
                            }
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
