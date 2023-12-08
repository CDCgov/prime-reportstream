import { Fieldset } from "@trussworks/react-uswds";

import {
    SettingFormFieldRow,
    validateJsonField,
} from "../../SettingFormField/SettingFormField";
import { SettingFormFieldsetProps } from "../SettingFormFieldset";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";
import { Textarea } from "../../../FormInput";

export interface SettingJsonFieldsetProps extends SettingFormFieldsetProps {}

export default function SettingJsonFieldset({
    children,
}: SettingJsonFieldsetProps) {
    const form = useSettingForm();
    return (
        <Fieldset>
            <SettingFormFieldRow>
                <Textarea {...form.register("_raw")} />
            </SettingFormFieldRow>
            {children}
        </Fieldset>
    );
}
