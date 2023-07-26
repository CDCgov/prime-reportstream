# Route

During this step the UP goes through every Full ELR receiver and checks if the message can be routed to them. It goes through the receiver’s quality, jurisdictional, routing and condition filters. If all the filters pass RS will route the message to the receiver and also remove any conditions that don’t match the condition filter.

Note: This step occurs after the Convert step and before the Translate step.

## Filters

Make sure to explain the default for each filter - explain that routing in RS depends on filters. If a receiver wants a message to go to them, it has to pass their filters. Additionally, the default filter also depends on the associated topic. (Already some data on this, needs updating)
[Reference](https://docs.google.com/spreadsheets/d/1xAfkzPqs4yA3tAdXW4JShtENTZW_kSW6rlH_Hjx-glI/edit#gid=0) for user settings requested upon onboarding

- Quality Filter
- RoutingFilter
- JurisdictionalFilter
- Processing Mode Filter
- Condition Filter
- Developer section on how they work with examples. Also explain in this section how they can now be put on separate lines. 
