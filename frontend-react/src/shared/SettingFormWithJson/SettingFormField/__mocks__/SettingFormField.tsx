import type { SettingFormFieldProps } from "../SettingFormField";

export default function SettingFormField({
    name,
    onChange,
    render: Children,
}: SettingFormFieldProps) {
    const onChangeHandler = jest.fn();

    return (
        <Children
            id={`test-${name}`}
            name={name}
            onChange={(e) => onChangeHandler(onChange(e))}
            mode={"edit"}
            defaultValue=""
            disabled={false}
        />
    );
}
