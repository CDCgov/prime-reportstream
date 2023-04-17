/**
 * Using the provided keys, match the unflattened props and inflate them into
 * a final new object to represent their intended structure.
 */
export function unflattenProps<O>(obj: O, keys: string | string[], sep = "__") {
    const keyList = Array.isArray(keys) ? keys : [keys];
    const newObj = {} as any;
    for (const k in obj) {
        const match = keyList.find((ck) => k.startsWith(`${ck}${sep}`));
        if (match) {
            const matchKeys = match.split(sep);
            let currentObj = newObj;
            for (const [i, matchKey] of matchKeys.entries()) {
                if (currentObj[matchKey] === undefined) {
                    currentObj[matchKey] = {};
                }
                currentObj = currentObj[matchKey];
                if (i + 1 === matchKeys.length) {
                    currentObj[k.substring(match.length + 2)] = obj[k];
                }
            }
        } else {
            (newObj as any)[k] = obj[k];
        }
    }
    console.log(obj, keys, newObj);
    return newObj;
}

/**
 * Helper to get storybook docgen to create controls and types based off
 * your needed nested props (ex: subComponentProps__requiredProperty ->
 * subComponentProps.requiredProperty). Unfortunately, this has to be in
 * a module that is imported by the story in order for storybook's
 * processing to work which means either living next to the original
 * component or in a dedicated module.
 *
 * Be sure to define a category for these custom controls so that they can
 * be visually separated from the real props.
 * ```
 * {
 *   argTypes: {
 *     subComponentProps__requiredProperty: {
 *       table: {
 *         category: "subComponentProps"
 *       }
 *     }
 *   }
 * }
 * ```
 */
export function withUnnestedProps<C, P>(
    Component: C extends React.ComponentType<infer OP>
        ? React.ComponentType<OP>
        : never,
    keys: string | string[]
) {
    return (
        props: C extends React.ComponentType<infer OP> ? OP & P : never,
        ref?: React.ForwardedRef<any>
    ) => <Component ref={ref} {...unflattenProps(props, keys)} />;
}
