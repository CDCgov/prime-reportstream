import { Checkbox, Label, TextInput, Textarea } from "@trussworks/react-uswds";
import { useEffect } from "react";

import { SettingFormFieldRow } from "../../SettingFormField/SettingFormField";
import { SettingFormFieldsetProps, filterHint } from "../SettingFormFieldset";
import {
    RSReceiver,
    SampleTiming,
    SampleTranslation,
    SampleTransports,
} from "../../../../config/endpoints/settings";
import ObjectExampleTooltip from "../../Tooltips/ObjectExampleTooltip";
import { useSettingForm } from "../../SettingFormContext/SettingFormContext";

export interface ReceiverFieldsetProps extends SettingFormFieldsetProps {}

export default function ReceiverFieldset({ children }: ReceiverFieldsetProps) {
    const { form, mode, registerJsonFields } = useSettingForm<
        RSReceiver,
        | "translation"
        | "jurisdictionalFilter"
        | "qualityFilter"
        | "routingFilter"
        | "conditionFilter"
        | "processingModeFilter"
        | "timing"
        | "transport"
    >();

    useEffect(() => {
        registerJsonFields(
            "translation",
            "jurisdictionalFilter",
            "qualityFilter",
            "routingFilter",
            "conditionFilter",
            "processingModeFilter",
            "timing",
            "transport",
        );
    }, [registerJsonFields]);
    return (
        <>
            <form.Field
                name="translation"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={field.name}>Translation</Label>
                                <ObjectExampleTooltip obj={SampleTranslation} />
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
                name="jurisdictionalFilter"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name} hint={filterHint}>
                                Jurisdictional Filter
                            </Label>
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
                name="qualityFilter"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name} hint={filterHint}>
                                Quality Filter
                            </Label>
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
                name="reverseTheQualityFilter"
                children={(field) => (
                    <SettingFormFieldRow>
                        <Checkbox
                            label={"Reverse the Quality Filter"}
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
            <form.Field
                name="routingFilter"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name} hint={filterHint}>
                                Routing Filter
                            </Label>
                        }
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
                name="processingModeFilter"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name} hint={filterHint}>
                                Processing Mode Filter
                            </Label>
                        }
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
                name="deidentify"
                children={(field) => (
                    <SettingFormFieldRow>
                        <Checkbox
                            label={"De-identify"}
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
            <form.Field
                name="timing"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={field.name}>Timing</Label>
                                <ObjectExampleTooltip obj={SampleTiming} />
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
                name="transport"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={field.name}>Transport</Label>
                                <ObjectExampleTooltip
                                    obj={SampleTransports}
                                    description="Field accepts ONE of allowed types from sample."
                                />
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
                name="externalName"
                children={(field) => (
                    <SettingFormFieldRow
                        label={
                            <Label htmlFor={field.name}>External Name</Label>
                        }
                    >
                        <TextInput
                            name={field.name}
                            type="text"
                            id={field.name}
                            value={field.state.value ?? ""}
                            disabled={mode === "readonly"}
                            onChange={(e) =>
                                field.handleChange(e.currentTarget.value)
                            }
                        />
                    </SettingFormFieldRow>
                )}
            />
            {children}
        </>
    );
}
