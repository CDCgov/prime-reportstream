import {
    Select as _Select,
    TextInput as _TextInput,
    Textarea as _Textarea,
} from "@trussworks/react-uswds";
import { ComponentType, ForwardRefExoticComponent, forwardRef } from "react";

/**
 * Typescript spaghetti function to allow natural ref usage with
 * react-uswds form input components
 */
function FixInputComponentRef<
    const T,
    P = T extends ComponentType<infer t> ? t : never,
>(
    C: P extends { inputRef?: any } ? ComponentType<P> : never,
): ForwardRefExoticComponent<Omit<P, "inputRef">> {
    return forwardRef((props, ref) => {
        const AnyC = C as unknown as ComponentType<{ inputRef?: any }>;
        return <AnyC {...props} inputRef={ref} />;
    }) as any;
}

export const Select = FixInputComponentRef(_Select);
export const Textarea = FixInputComponentRef(_Textarea);
export const TextInput = FixInputComponentRef(_TextInput);
