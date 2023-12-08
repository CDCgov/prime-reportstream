import { Fieldset, Label } from "@trussworks/react-uswds";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { SettingFormFieldsetProps, filterHint } from "../SettingFormFieldset";
import { jurisdictionChoices } from "../../../../config/endpoints/settings";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";
import { Select, TextInput, Textarea } from "../../../FormInput";

export interface OrganizationFieldsetProps extends SettingFormFieldsetProps {}

export default function OrganizationFieldset({
    children,
}: OrganizationFieldsetProps) {
    const form = useSettingForm();
    console.log("org form", form);
    const field = { name: "todo" };

    return (
        <Fieldset>
            <SettingFormFieldRow
                label={<Label htmlFor={field.name}>Jurisdiction</Label>}
            >
                <Select {...form.register("jurisdisction")}>
                    {jurisdictionChoices.map((c) => (
                        <option key={c} value={c}>
                            {c}
                        </option>
                    ))}
                </Select>
            </SettingFormFieldRow>
            <SettingFormFieldRow
                label={<Label htmlFor={field.name}>County Name</Label>}
            >
                <TextInput type="text" {...form.register("countyName")} />
            </SettingFormFieldRow>
            <SettingFormFieldRow
                label={<Label htmlFor={field.name}>State Code</Label>}
            >
                <TextInput type="text" {...form.register("stateCode")} />
            </SettingFormFieldRow>
            <SettingFormFieldRow
                label={
                    <Label htmlFor={field.name} hint={filterHint}>
                        Filters
                    </Label>
                }
            >
                <Textarea {...form.register("filters")} />
            </SettingFormFieldRow>
            {children}
        </Fieldset>
    );
}
