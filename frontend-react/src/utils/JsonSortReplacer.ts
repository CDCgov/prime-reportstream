export const jsonSortReplacer = (key: string, value: any) => {
    if (!(value instanceof Object)) {
        return value;
    }
    if (value instanceof Array) {
        return value.sort();
    } else {
        return Object.keys(value)
            .sort()
            .reduce((sorted: { [key: string]: any }, k: string) => {
                sorted[k] = value[k];
                return sorted;
            }, {});
    }
};
