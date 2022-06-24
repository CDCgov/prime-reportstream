# ReportStream Table Component

The ReportStream configurable table allows us to configure tables around our data shapes
and not worry about handling any of the UI creation. Additionally, extension objects can be
created and assigned as optional props to provide further utility (e.g. `FilterManager`)

The heavy lifting is configuring your columns, the table handles iterating the rows, which will
likely be fetched from our API, and generating the UI based on the config.

## Column config

Everything is configurable through the `ColumnConfig` interface. This, too, can be easily 
extended and utilized as needed to cover more use cases. You will create an array of these
configuration interfaces to pass into your table.

```typescript
export interface ColumnConfig {
    dataAttr: string; // Object field to render
    columnHeader: string;
    feature?: ColumnFeature; // Configurable column features
    sortable?: boolean;
    valueMap?: Map<string | number, any>; // Provide a map of alternate values
    transform?: Function; // Transform your display value
    editable?: boolean; // Raw data value can be edited
    localSort?: boolean; // Sorting will only be applied locally (in-table, not API)
}
```

### `dataAttr`

This is the attribute of the row object you wish to render. For example, `{ foo: "bar" }` would
be configured with `dataAttr: "foo"` to achieve rendering of the value "bar" in the cell. Any object
getting passed to the table for rendering becomes a `TableRow`, which is just a fancy way of saying it
becomes string-indexed, so we can access attributes with bracket-notation instead of dot-notation like
this:`row[dataAttr]`.

### `columnHeader`

Used to set the `<th>` text.

### `feature (optional)`

Features can be created and checked to conditionally render a unique type of column that looks
and/or behaves in a non-typical way. Two examples are `LinkableColumn` and `ActionableColumn`.
Both of these allow for different unique uses of the table cell, for a nav link and button respectively.

You can create your own feature, add it to the `ColumnFeature` union type, and conditionally render
it using the tools in `<ColumnData>`. Specifically, you'll want to check for your feature's main property
using `hasFeature()` and passing the string accessor for the attribute you're checking. In the case of
a `LinkableColumn`, this would be `hasfeature("link")`.

```typescript jsx
// Create the feature
export interface UniqueColumn { color: string }
// Add it to the union type
type ColumnFeature = ActionableColumn | LinkableColumn | UniqueColumn;
// Validate it in <Table> -> <ColumnData>
if (hasFeature("color")) return <Text color={column.feature.color}>{row[dataAttr]}</Text>
```

### `sortable && localSort (optional)`

> A `FilterManager` is required by the `Table` to utilize sorting

We have two uses for sorting tables; on one hand, we'll want a server-side sort (see `SubmissionsTable`)
and on the other we might want to sort locally, just merely changing the order of the elements already
fetched. For this, we have the `sortable` and `localSort` options. 

The main flag is `sortable`. This ensures you're able to link into the filter manager and update the sort
settings when clicking a sortable column's header. `SortSettings` has a `local` flag that, when a column
is configured with `localSort`, gets set to true and will instead apply the sort to the table data. This
can be found in `Table > memoizedRows`

Local sort has support for `string | number` **only** for now, and can be extended to include sort functions
that consider objects with multiple attributes.

### `valueMap (optional)`

If you are receiving values back from the server that aren't user-friendly, and wish to map them to more
user-friendly values, you can provide a map to do so. For instance, let's say you receive an object who's
`status` field is a numeric value, and you want to map this to a meaningful value for the user:

```typescript
const statusCodes = new Map<number, string>([
    [200, "Success"],
    [404, "Not found"]
])

const config: ColumnConfig = {
    ...config,
    valueMap: statusCodes
}
```

### `transform (optional)`

If you are receiving values back from the server that need to be transformed in some way, adding a value
to this attribute in your configuration will enable `ColumnData` to transform the display value while
maintaining the raw value in case this field needs to also be editable.

```typescript
const makeLocaleString = (s: string) => {
    return new Date(s).toLocaleString();
};

const config: ColumnConfig = {
    ...config,
    transform: makeLocaleString
}
```

### `editable (optional)`

This flag on the column config, alongside `enableEditableRows` on the `Table` component, creates an editable
value in a row. If the value is marked editable in the config but there's no way to edit it, check that the
table has `enableEditableRows` marked true.

> `valueMap` and `transform` rows can be edited. When you edit them, the raw data value will be shown in the input
> instead of the mapped value. Then, the saved value will be re-transformed or re-mapped.

## Table config

Now that the heavy lifting is done, we can handle the `TableConfig`. Plug in your `ColumnConfig[]` to the
table config via the `columns` attribute. Our `rows` attribute will be passed a reactive variable that maintains
our fetch response.

```typescript
const submissions = useResource(...)

const submissionsConfig: TableConfig = {
    columns: columns,
    rows: submissions,
};
```

Once you have data and a config, you'll want to pass these into the Table. It will
handle the rendering for you.

```typescript jsx
<Table config={submissionsConfig} />
```

### Additional configuration

In some cases, additional information or action is required alongside your table. For intance, if we have a table
containing values in a set, and wish to add one to it, we need a `DatasetAction` to handle it for us. Or, if we are
presenting this table to a user, perhaps a legend and title would help communicate the information better. We can 
configure all of this through the `TableConfig` interface.

#### Title

Starting with the easiest, and most common use, a title is a great way of telling a user what the table is for. To
add one, just add the `title` prop to your Table creation!

```typescript jsx
<Table title="Submissions History" config={submissionsConfig} />
```

#### Legend

Because a legend's design need might be entirely unique table to table, we leave it to the parent to pass in
a unique `<Legend>`. This prop is typed as a `ReactNode` so any UI element type _should_ work. 

```typescript jsx
const Legend = () => {
    return(
        <ul>
            <li>Item 1: This</li>
            <li>Item 2: That</li>
            <li>Item 3: Everything Inbetween</li>
        </ul>
    )
}

<Table 
    title="Submissions History" 
    legend={<Legend />}
    config={submissionsConfig} 
/>
```

#### DatasetAction

Lastly, if you need an action performed on the entire dataset displayed in the table, you can perform that
through an optional `DatasetAction`, created and/or passed through props by the parent to render a button and
have it perform the callback onClick. A common example might be adding an item to the dataset, or refreshing
it.

```typescript jsx
const addItem = () => submissions.push({
    attr1: "sample",
    attr2: "value"
});

<Table 
    title="Submissions History" 
    legend={<Legend />}
    datasetAction={{
        label: "Add item",
        method: addItem
    }}
    config={submissionsConfig} 
/>
```

Here, you'll see an inline action object, but the `DatasetAction` interface takes a string label and a function as
a method. 

> It's important to note: right now, there's no way to pass parameters into this function when it's called
from the table.

# Add-ons

## Using the FilterManager & Filter UI

> Note: `tech-debt` item filed to make this a generative use instead of the need to manually
> add it to your table page

You'll notice that the `SubmissionTable` example uses a filter manager as well.
What is that? A filter manager is a hook that manages filter state. It currently
supports:
- Date range
- Sort order and column
- Page size

To utilize these filters, you'll need to first set up a filter manager in your
parent component using the `useFilterManager` hook, then pass it into both
the table and a `TableFilter` component.

```typescript jsx
import useFilterManager from "./UseFilterManager";

const fm = useFilterManager()

return (
    <>
        <TableFilters
            filterManager={fm}
        />
        <Table
            config={submissionsConfig}
            filterManager={fm}
        />
</>
)
```

## Cursors and Pagination

You can also add pagination through the cursor manager object. The setup is
very similar to setting up the filter manager; instantiate one, pass it into the
filters and table UI, and the rest gets handled by the components:

```typescript jsx
import useFilterManager from "./UseFilterManager";
import useCursorManager from "./UseCursorManager";

const fm = useFilterManager()
const cm = useCursorManager()

return (
    <>
        <TableFilters
            filterManager={fm}
            cursorManager={cm}
        />
        <Table
            config={submissionsConfig}
            filterManager={fm}
            cursorManager={cm}
        />
    </>
)
```

## Range filtering and pagination on API call

If you do implement cursor-based pagination through the cursor manager, you'll want
to ensure your API call is properly set up for that. Your call might look something
like this to start:

```typescript
const submissions: SubmissionsResource[] = useResource(
    SubmissionsResource.list(),
    {
        organization: getStoredOrg(),
        cursor: filterManager.rangeSettings.start,
        endCursor: filterManager.rangeSettings.end,
        pageSize: filterManager.pageSettings.size + 1, // Pulls +1 to check for next page
        sort: filterManager.sortSettings.order,
        showFailed: false, // No plans for this to be set to true
    }
);
```

When sorting and paginating, this logic needs to become dynamic. For this, there's a
helper function that'll process the ranges and cursor properly. All you need to do is
pass in the values required, and it spits the right one out:

```typescript
import {RangeField} from "./UseDateRange";
import {cursorOrRange} from "./UseFilterManager";

const submissions: SubmissionsResource[] = useResource(
    SubmissionsResource.list(),
    {
        // ...
        cursor: cursorOrRange(
            filterManager.sortSettings.order,
            RangeField.START,
            cursors.current,
            filterManager.rangeSettings.start
        ),
        endCursor: cursorOrRange(
            filterManager.sortSettings.order,
            RangeField.END,
            cursors.current,
            filterManager.rangeSettings.end
        ),
        // ...
    }
);
```


