import { FocusTrapProps } from "focus-trap-react";
import React from "react";
import { vi } from "vitest";

const FocusTrap = (await vi.importActual("focus-trap-react")).default as React.ComponentType<FocusTrapProps>;

/**
 * Override displayCheck for testing. See: https://github.com/focus-trap/tabbable#testing-in-jsdom
 */
const FixedComponent = ({ focusTrapOptions, ...props }: FocusTrapProps) => {
    const fixedOptions = { ...focusTrapOptions };
    fixedOptions.tabbableOptions = {
        ...fixedOptions.tabbableOptions,
        displayCheck: "none",
    };
    return <FocusTrap {...props} focusTrapOptions={fixedOptions} />;
};
export default FixedComponent;
