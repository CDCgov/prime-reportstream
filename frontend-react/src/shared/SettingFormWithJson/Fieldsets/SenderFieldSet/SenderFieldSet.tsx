import {
    Checkbox,
    Label,
    Select,
    TextInput,
    Textarea,
} from "@trussworks/react-uswds";
import { useEffect } from "react";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { SettingFormFieldsetProps } from "../SettingFormFieldset";
import {
    RSSender,
    SampleScopedJwks,
    formatChoices,
    processingTypeChoices,
} from "../../../../config/endpoints/settings";
import ObjectExampleTooltip from "../../Tooltips/ObjectExampleTooltip";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";

export interface SenderFieldsetProps extends SettingFormFieldsetProps {}

export default function SenderFieldset({ children }: SenderFieldsetProps) {
    const { form, mode, registerJsonFields } = useSettingForm<
        RSSender,
        "keys"
    >();

    useEffect(() => {
        registerJsonFields("keys");
    }, [registerJsonFields]);

    return (
        <>
            <form.Field
                name="format"
                children={(field) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={field.name}>Format</Label>}
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
                            {formatChoices.map((c) => (
                                <option key={c} value={c}>
                                    {c}
                                </option>
                            ))}
                        </Select>
                    </SettingFormFieldRow>
                )}
            />
            <form.Field
                name="schemaName"
                children={(field) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={field.name}>Schema Name</Label>}
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
            <form.Field
                name="keys"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={field.name}>Keys</Label>
                                <ObjectExampleTooltip obj={SampleScopedJwks} />
                            </>
                        }
                    >
                        <Textarea
                            name={field.name}
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
            <form.Field
                name="processingType"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name}>Processing Type</Label>
                        }
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
                            {processingTypeChoices.map((c) => (
                                <option key={c} value={c}>
                                    {c}
                                </option>
                            ))}
                        </Select>
                    </SettingFormFieldRow>
                )}
            />
            <form.Field
                name="allowDuplicates"
                children={(field) => (
                    <SettingFormFieldRow>
                        <Checkbox
                            label={"Allow Duplicates"}
                            name={field.name}
                            id={field.name}
                            checked={field.state.value}
                            disabled={mode === "readonly"}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.checked)
                            }
                        />
                    </SettingFormFieldRow>
                )}
            />
            {children}
        </>
    );
}
