# Website API communication patterns

## Introduction

As we continue to develop more features for the ReportStream website, we're inevitably going to need to add new API endpoints to support said features.  We should consequently create consistent, predictable, and scalable patterns for communicating between the website and the API so we can avoid unnecessary overhead when adding new endpoints. 

## Goals

The purpose of this proposal is to align on communication between ReportStream API and the website, including:
- HTTP response codes
- Req/res structures
    - Resource structures
    - Collection structures

Having a consistent approach to the above would provide some predictability in implementation as we continue to build out the website, and it would help to simplify logic in common UX patterns like search filters and pagination.  (For example, UsePagination.ts contains [visual slot logic](https://github.com/CDCgov/prime-reportstream/blob/master/frontend-react/src/hooks/UsePagination.ts#L26-L84) that could be drastically simplified if there were, say, a `totalCount` included in the response payload.)

**NOTE: This proposal is only intended for the endpoints used by the ReportStream website.**  Although it can be argued that we'd benefit from having a consistent response payload structure across the board, updating our user-facing API signatures may create too much cascading work for our users at the moment.  (However, this could be an item we raise again if we choose to implement API versioning down the line.)

## Current examples

### Resource

`GET /api/waters/report/72b3efe2-da70-4f3d-b531-32130b79dcd5/delivery`


```json
{
    "deliveryId" : 81,
    "batchReadyAt" : "2023-03-29T23:53:00.488Z",
    "expires" : "2023-04-28T23:53:00.488Z",
    "receiver" : "ignore.CSV",
    "reportId" : "72b3efe2-da70-4f3d-b531-32130b79dcd5",
    "topic" : "covid-19",
    "reportItemCount" : 40,
    "fileName" : "az-covid-19-72b3efe2-da70-4f3d-b531-32130b79dcd5-20230329195300.csv",
    "fileType" : "CSV"
}
```

### Collection

`GET /api/waters/org/ignore.CSV/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61`

```json
[{
    "deliveryId" : 81,
    "batchReadyAt" : "2023-03-29T23:53:00.488Z",
    "expires" : "2023-04-28T23:53:00.488Z",
    "receiver" : "ignore.CSV",
    "reportId" : "72b3efe2-da70-4f3d-b531-32130b79dcd5",
    "topic" : "covid-19",
    "reportItemCount" : 40,
    "fileName" : "az-covid-19-72b3efe2-da70-4f3d-b531-32130b79dcd5-20230329195300.csv",
    "fileType" : "CSV"
}, {
    "deliveryId" : 30,
    "batchReadyAt" : "2023-03-29T23:52:06.639Z",
    "expires" : "2023-04-28T23:52:06.639Z",
    "receiver" : "ignore.CSV",
    "reportId" : "e1676dcf-ddf7-4d22-aa11-13c8a81f0318",
    "topic" : "covid-19",
    "reportItemCount" : 30,
    "fileName" : "az-covid-19-e1676dcf-ddf7-4d22-aa11-13c8a81f0318-20230329195207.csv",
    "fileType" : "CSV"
}]
```

## Proposed changes

### HTTP response codes

The following is a non-exhaustive list of HTTP response codes that should commonly be used for the ReportStream website.  This should be a rough guideline of which HTTP response code to return from the API for all future endpoints.

| Code | Name                  | Explanation                                                                                      | Example                                                            
|------|-----------------------|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------| 
| 200  | OK                    | Success with content                                                                             | Admin successfully requests settings for a particular Organization |
| 201  | Created               | Success with new resource created                                                                | Admin successfully creates a new Sender/Receiver                   |
| 400  | Bad Request           | Error; user submitted invalid/malformed data                                                     | Admin submits malformed Sender/Receiver settings data              
| 401  | Unauthorized          | Error; user is not currently authorized to access the resource (may need to re-authenticate)     | Receiver requests deliveries with outdated Okta token              
| 403  | Forbidden             | Error; user is never authorized to access the resource (re-authentication won't affect anything) | Receiver requests another Receiver's deliveries                    
| 404  | Not Found             | Error; requested resource does not exist                                                         | Admin requests Receivers for a non-existent Organization           |
| 500  | Internal Server Error | Unhandled exception                                                                              | Catch-all for all unhandled exceptions                             |

NOTE: This table does not include common HTTP response codes commonly used as navigation signals, such as `301 Moved Permanently` or `308 Permanent Redirect`, which are not currently applicable to the website due to its single-page application (SPA) approach.  This may change in the future if we migrate to a more traditional website architecture.

### Req/res structures

Whether they include a single entity or a list of entities, **all responses should take the form of a JSON object as opposed to a JSON array**.  This is primarily for better scalability so that we can more easily add additional properties and metadata if we need to in the future.  (It incidentally has the added benefit of being more resistant to [JSON hijacking](https://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx/), but this is no longer a concern in modern browsers.)

#### Resource req/res (`show` of an individual entity)

Resource requests paths should take the form of `*/<resources>/<id>`, where:
- `resources` is the entity view, facade, or database table
- `id` is a publicly displayable identifier (should at least be an indexed field if querying from a table)

Resource responses should take the form of a JSON object:

```json
{
    "property1": ...,
    "property2": ...,
    "property3": ...,
    // other resource data     
    ...
}
```

##### Example

`GET /api/settings/organizations/ignore`

```json
{
    "name" : "ignore",
    "description" : "FOR TESTING ONLY",
    "jurisdiction" : "FEDERAL",
    "filters" : [],
    "featureFlags" : [],
    "keys" : [],
    "version" : 0,
    "createdBy" : "local@test.com",
    "createdAt" : "2023-03-29T23:49:59.996Z"
}
```

The above is our generally our current approach to querying individual resources so no significant changes should have to be made.

#### Collections (`index` of lists of entities)

Collection request paths should take the form of `*/<resources>`, where:

- `resources` refers to the entity view, facade, or database table

Collection responses should take the form of a JSON object:

```json
{
    "meta": {
        "type": <resource_type>,
        "totalCount": <total_count>
    },
    
    "data": [
        {
            "property1": ...,
            "property2": ...,
            "property3": ...,
            // other resource data
            ... 
        },
        // other resource objects
        ...
    ]
}
```

`meta` should include all necessary collection metadata.  In a basic collection response, it should at least include the following values:

- `type`: the type of the requested resources
- `totalCount`: the total count of the requested resources

There should be more values for special collections, which will be elaborated on below.

##### Example 

`GET /api/actions`

```json
{
    "meta": {
        "type": "Action",
        "totalCount": 100
    },

    "data": [
        {
            "action_id": "1",
            "action_name": "batch",
            "action_params": "receiver&BATCH&ignore.FULL_ELR&false",
            "action_result": null,
            ...
        },
        {
            "action_id": "2",
            "action_name": "batch",
            "action_params": "receiver&BATCH&ignore.CSV&false",
            "action_result": "Success: merged 6 reports into 1 reports",
            ...
        },
        ...
    ]
}
```

#### Paginated collections

Paginated collection request paths should take the form of `*/<resources>?page=<page>&pageSize=<pageSize>`, where:

- `page` is the requested page (e.g., `1`, `2`, `10`)
- `pageSize` is the size of the request pages (e.g., `10`, `20`, `50`)

Paginated collection responses should take the form of:

```json
{
    "meta": {
        "type": <resource_type>,
        "totalCount": <total_count>,
        
        // pagination-specific values
        "totalPages": <count_of_total_pages>,
        "previousPage": <previous_page_number>,
        "nextPage": <next_page_number>,
    },
    "data": [
        {
            "property1": ...,
            "property2": ...,
            "property3": ...,
            // other resource data
            ...
        },
        // other resource objects
        ...
    ]
}
```

The `meta` for paginated collection responses should include the following values:

- `totalPages` is the count of total pages
- `previousPage` is the previous page number (`null` if on the first page)
- `nextPage` is the next page number (`null` if on the last page)

##### Example

`GET /api/actions?page=1&pageSize=50`

```json
{
    "meta": {
        "type": "Action",
        "totalCount": 100,

        // pagination-specific values
        "totalPages": 2,      // (100 / 50 = 2)
        "previousPage": null, // on first page so previous page is null 
        "nextPage": 2
    },

    "data": [
        {
            "action_id": "1",
            "action_name": "batch",
            "action_params": "receiver&BATCH&ignore.FULL_ELR&false",
            "action_result": null,
            ...
        },
        {
            "action_id": "2",
            "action_name": "batch",
            "action_params": "receiver&BATCH&ignore.CSV&false",
            "action_result": "Success: merged 6 reports into 1 reports",
            ...
        },
        ...
    ]
}
```

#### Sorted collections

Sorted collection request paths should take the form of `*/<resources>?sortDir=<sortDir>&sortColumn=<sortColumn>`, where:

- `sortDir` is the order of sorting (`ASC` or `DESC`)
- `sortColumn` is the name of the sorted column (e.g., `id`, `name`, `expiresAt`)
    - In the case of sorting by multiple columns, they will be comma-separated with descending priority (e.g., `sortColumn=id,name,expiresAt` will sort by `id` first, `name` second, and `expiresAt` third) 

(Note the updated capitalization.)

Sorted collection responses should take the typical form of collection responses as described above.

#### Filtered collections

Filtering may change from endpoint to endpoint based on the nature of the requested data.  (The collection in the 'Current examples' section is an example of filtering wherein the requested data is filtered by timestamp.)  

In any case, `meta` may be used to hold filter-specific values for better visibility into the total set of resources:

```json
{
    "meta": {
        "type": <resource_type>,
        "totalCount": <total_count>,
        
        // filter-specific values
        "totalFilteredCount": <total_count_of_filtered_items>,
        
    }
}
```

Here, the extra meta values may include:

- `totalFilteredCount` is the total filtered count of requested resources (differentiated from `totalCount`, which is the total _unfiltered_ count of requested resources)

#### Combined collections

Collection req/res may need to combine the functionalities listed above so there could be, for example, a request for a sorted, filtered collection with pagination.  The collection in the 'Current examples' section is an example of this need:

`GET /api/waters/org/ignore.CSV/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61`

However, this would require changes to the query parameters and the response structure to align with the proposed changes in this doc:

- Update `sortdir` to `sortDir`
- Add `sortColumn` query parameter
- Add `page` query parameter
- Add response `meta`