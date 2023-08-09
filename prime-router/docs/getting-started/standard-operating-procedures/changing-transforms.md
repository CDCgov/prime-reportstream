# Changing/Updating Sender/Receiver Transforms

The purpose of this document is to explain where to make a change when you 
need to update a transform. If you need more information please read 
[Convert/Translate](../../universal-pipeline/convert-translate.md)

```mermaid
flowchart LR
    box1[Is it a part of a spec?]-->yes1[Yes]-->box2[Put it in the resource];
    box1-->no1[No]-->box3[Is this specific to one receiver?];
    box3-->yes2[Yes]-->box4[Receiver Transform];
    box3-->no2[No]-->box5[Will this need to be applied to all senders?];
    box5-->yes3[Yes]-->box6[Default sender transform];
    box5-->no3[No]-->box7[Specific sender transform];
```