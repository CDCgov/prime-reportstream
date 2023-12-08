import { Fieldset, Label } from "@trussworks/react-uswds";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { RSSetting } from "../../../../config/endpoints/settings";
import { SettingFormFieldsetProps } from "../SettingFormFieldset";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";
import { TextInput } from "../../../FormInput";

export interface SettingFieldsetProps extends SettingFormFieldsetProps {}

export default function SettingFieldset({ children }: SettingFieldsetProps) {
    const form = useSettingForm<RSSetting>();
    return (
        <Fieldset>
            <SettingFormFieldRow
                label={
                    <Label htmlFor={"name"} requiredMarker>
                        Name
                    </Label>
                }
            >
                <TextInput {...form.register("name")} type="text" />
            </SettingFormFieldRow>
            <SettingFormFieldRow
                label={<Label htmlFor={"description"}>Description</Label>}
            >
                <TextInput {...form.register("description")} type="text" />
            </SettingFormFieldRow>
            {children}
        </Fieldset>
    );
}
