const vc = (window as any)._virtualConsole;
const vcEmit = vc.emit;

export function restoreVirtualConsole() {
    vc.emit = vcEmit;
}

/**
 * React will always emit a console error even when caught by an Error Boundary.
 * This is a hack to shut jsdom up about it.
 */
export function silenceVirtualConsole() {
    vc.emit = () => void 0;
    return restoreVirtualConsole;
}

export default silenceVirtualConsole;
