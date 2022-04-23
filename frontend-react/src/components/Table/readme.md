# ReportStream Table Component

The ReportStream configurable table covers all our current use cases in the ReportStream
web application: static content, content with filters, and content with pagination. Most
importantly, it's easily configured from a parent component. Let's take `SubmissionTable`
as an example.

## Configuration

First, you'll want to create your column configurations. In the case of `SubmissionTable`,
we are rendering a `SubmissionResource` in each row, so our columns should be representative
of the data fields on that object we wish to show.

A column config follows this shape:
```typescript
interface ColumnConfig {
    /* Field name on the object */
    dataAttr: string;
    /* Displayed string value for header */
    columnHeader: string;
    /* Makes column sortable, ascending/descending */
    sortable?: boolean;
    /* Makes column value a hyperlink */
    link?: boolean;
    /* Prepends link with some base path (no root needed) */
    linkBasePath?: string;
    /* Optionally map the hyperlink to a different attribute 
    * in the object Defaults to dataAttr */
    linkAttr?: string;
    /* Map raw values to a nicer display value */
    valueMap?: Map<string | number, any>;
    /* Perform a transformation on the data before displaying */
    transform?: Function;
}
```

What makes `SubmissionTable` such a great example for us is that it utilizes most, if not all,
of these configuration attributes in some way. Let's take a look at the configuration for that
table:

```typescript
const transformDate = (s: string) => {
    return new Date(s).toLocaleString();
};

const columns: Array<ColumnConfig> = [
    {
        dataAttr: "id",
        columnHeader: "Report ID",
        link: true,
        linkBasePath: "/submissions", // links to /submissions/${id}
    },
    {
        dataAttr: "timestamp",
        columnHeader: "Date/time submitted",
        sortable: true,
        transform: transformDate, // transforms date to locale string
    },
    { 
        dataAttr: "externalName", 
        columnHeader: "File" 
    },
    { 
        dataAttr: "reportItemCount", 
        columnHeader: "Records" 
    },
    {
        dataAttr: "httpStatus",
        columnHeader: "Status",
        valueMap: new Map([[201, "Success"]]), // maps http status to string value
    },
];
```

Once your column configuration array is ready, you'll pass it, along with your data
array, into a `TableConfig` object:

```typescript
const submissionsConfig: TableConfig = {
    columns: columns,
    rows: submissions,
};
```

From here, you're ready to give it to your table!

## Render the table

Once you have data and a config, you'll want to pass these into the Table. It will
handle the rendering for you.

```typescript jsx
<Table config={submissionsConfig} />
```

# Add-ons

## Using the FilterManager & Filter UI

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


