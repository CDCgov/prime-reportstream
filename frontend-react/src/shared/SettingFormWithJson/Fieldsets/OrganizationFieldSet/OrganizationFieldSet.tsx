import {
    Fieldset,
    Label,
    Select,
    TextInput,
    Textarea,
} from "@trussworks/react-uswds";
import { useEffect } from "react";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { SettingFormFieldsetProps, filterHint } from "../SettingFormFieldset";
import {
    RSOrganization,
    jurisdictionChoices,
} from "../../../../config/endpoints/settings";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";

export interface OrganizationFieldsetProps extends SettingFormFieldsetProps {}

export default function OrganizationFieldset({
    children,
}: OrganizationFieldsetProps) {
    const { form, mode, registerJsonFields } = useSettingForm<
        RSOrganization,
        "filters"
    >();

    useEffect(() => {
        registerJsonFields("filters");
    }, [registerJsonFields]);

    return (
        <Fieldset>
            <form.Field
                name="jurisdiction"
                children={(field) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={field.name}>Jurisdiction</Label>}
                    >
                        <Select
                            name={field.name}
                            id={field.name}
                            value={field.state.value}
                            disabled={mode === "readonly"}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value)
                            }
                        >
                            {jurisdictionChoices.map((c) => (
                                <option key={c} value={c}>
                                    {c}
                                </option>
                            ))}
                        </Select>
                    </SettingFormFieldRow>
                )}
            />
            <form.Field
                name="countyName"
                children={(field) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={field.name}>County Name</Label>}
                    >
                        <TextInput
                            name={field.name}
                            type="text"
                            id={field.name}
                            value={field.state.value}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value)
                            }
                            disabled={mode === "readonly"}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <form.Field
                name="stateCode"
                children={(field) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={field.name}>State Code</Label>}
                    >
                        <TextInput
                            name={field.name}
                            type="text"
                            id={field.name}
                            value={field.state.value}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value)
                            }
                            disabled={mode === "readonly"}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <form.Field
                name="filters"
                onChangeAsyncDebounceMs={500}
                onChangeAsync={(value) => {
                    try {
                        JSON.parse(value);
                    } catch (e: any) {
                        return e.message;
                    }
                }}
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name} hint={filterHint}>
                                Filters
                            </Label>
                        }
                    >
                        <Textarea
                            name={field.name}
                            value={field.state.value}
                            id={field.name}
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
