import { useState } from "react";

/**
 * Uses a trick to force throwing an error within a synchronous context to ensure
 * all errors can be caught by an Error Boundary.
 */
export default function useAsyncSafeCallback(
    cb: (...args: any[]) => void | Promise<void>,
) {
    const [, setState] = useState();

    return async (...args: any[]) => {
        try {
            await cb(...args);
        } catch (e) {
            // Throwing inside the setter function allows
            // the error to be catchable by Error Boundaries
            setState(() => {
                throw e;
            });
        }
    };
}
