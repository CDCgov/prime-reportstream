import {
    Grid,
    Label,
    Select,
    TextInput,
    Textarea,
} from "@trussworks/react-uswds";
import { ChangeEvent, PropsWithChildren } from "react";

import { DisplayMeta } from "../../../../components/Admin/DisplayMeta";
import { ObjectTooltip } from "../../../../components/tooltips/ObjectTooltip";
import {
    SampleFilterObject,
    jurisdictionChoices,
} from "../../../../utils/TemporarySettingsAPITypes";
import {
    SettingFormField,
    SettingFormFieldRow,
} from "../../SettingFormField/SettingFormField";

export interface OrganizationFieldsetProps extends PropsWithChildren {}

export default function OrganizationFieldset({
    children,
}: OrganizationFieldsetProps) {
    return (
        <>
            <Grid row>
                <Grid col={3}>Meta:</Grid>
                <Grid col={9}>
                    <DisplayMeta metaObj={{} as any} />
                    <br />
                </Grid>
            </Grid>
            <SettingFormField
                name="jurisdiction"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={id}>Jurisdiction</Label>}
                    >
                        <Select
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                            title="Jurisdiction"
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
            <SettingFormField
                name="countyName"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={id}>County Name</Label>}
                    >
                        <TextInput
                            name={name}
                            type="text"
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="stateCode"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={<Label htmlFor={id}>State Code</Label>}
                    >
                        <TextInput
                            name={name}
                            type="text"
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            <SettingFormField
                name="filters"
                jsonType="field"
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                    e.currentTarget.value
                }
                render={({
                    defaultValue,
                    id,
                    name,
                    onChange,
                    disabled,
                    className,
                }) => (
                    <SettingFormFieldRow
                        label={
                            <>
                                <Label htmlFor={id}>Filters</Label>
                                <ObjectTooltip obj={new SampleFilterObject()} />
                            </>
                        }
                    >
                        <Textarea
                            name={name}
                            id={id}
                            defaultValue={defaultValue}
                            onBlur={onChange}
                            disabled={disabled}
                            className={className}
                        />
                    </SettingFormFieldRow>
                )}
            />
            {children}
        </>
    );
}
