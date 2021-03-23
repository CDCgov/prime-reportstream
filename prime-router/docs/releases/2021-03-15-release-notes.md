#  Report Stream March 15, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### New `/api/reports` options to support back filling of results

Two changes to the `/api/reports` end-point to assist in sending reports to a particular receiver. This is type action will typically be done when a new receiver is defined after a results are received by the hub. 

- `SendImmediately` value for the `option` query parameter to bypass any timing found on the receiver.
- `routeTo` query parameter restricts the list of receivers which a report can be routed to. 
  
The `routeTo` parameter **does not** bypass the jurisdictional filters of the receiver, so results meant for one receiver still cannot be sent to another receiver.