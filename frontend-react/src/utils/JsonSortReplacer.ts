export const jsonSortReplacer = (key: string, value: any) => {
    return value instanceof Object && !(value instanceof Array)
        ? Object.keys(value)
              .sort()
              .reduce((sorted: { [key: string]: any }, key: string) => {
                  sorted[key] = value[key];
                  return sorted;
              }, {})
        : value;
};
