import { Fieldset, Label, TextInput } from "@trussworks/react-uswds";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { RSSetting } from "../../../../config/endpoints/settings";
import { SettingFormFieldsetProps } from "../SettingFormFieldset";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";

export interface SettingFieldsetProps extends SettingFormFieldsetProps {}

export default function SettingFieldset({ children }: SettingFieldsetProps) {
    const { form, mode } = useSettingForm<RSSetting>();
    return (
        <Fieldset>
            <form.Field
                name="name"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name} requiredMarker>
                                Name
                            </Label>
                        }
                    >
                        <TextInput
                            name={field.name}
                            type="text"
                            id={field.name}
                            value={field.state.value}
                            disabled={mode === "readonly" || mode === "edit"}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value)
                            }
                        />
                    </SettingFormFieldRow>
                )}
            />
            <form.Field
                name="description"
                children={(field) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={field.name}>Description</Label>}
                    >
                        <TextInput
                            name={field.name}
                            type="text"
                            id={field.name}
                            value={field.state.value}
                            disabled={mode === "readonly"}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value)
                            }
                        />
                    </SettingFormFieldRow>
                )}
            />
            {children}
        </Fieldset>
    );
}
