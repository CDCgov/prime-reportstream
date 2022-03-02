export const jsonSortReplacer = (key: string, value: any) => {
    return value instanceof Object && !(value instanceof Array)
        ? Object.keys(value)
              .sort()
              .reduce((sorted: { [key: string]: any }, k: string) => {
                  sorted[k] = value[k];
                  return sorted;
              }, {})
        : value;
};
