/**
 * Type-safe way to ensure refs are initialized only once.
 */
export function getService<T>(ref: React.MutableRefObject<T>, initFn: () => Exclude<T, null>) {
    if (ref.current !== null) {
        return ref.current;
    }
    const service = initFn();
    ref.current = service;
    return service;
}
