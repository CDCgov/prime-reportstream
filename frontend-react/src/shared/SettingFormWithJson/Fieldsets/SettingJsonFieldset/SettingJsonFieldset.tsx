import { Fieldset, Textarea } from "@trussworks/react-uswds";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { SettingFormFieldsetProps } from "../SettingFormFieldset";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";

export interface SettingJsonFieldsetProps extends SettingFormFieldsetProps {}

export default function SettingJsonFieldset({
    children,
}: SettingJsonFieldsetProps) {
    const { form, mode } = useSettingForm();
    return (
        <Fieldset>
            <form.Field
                name="_raw"
                children={(field) => (
                    <SettingFormFieldRow>
                        <Textarea
                            id={field.name}
                            name={field.name}
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
