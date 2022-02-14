**Dev notes about content directory**

Files in this directory should be imported by react. The result is that the file considered "dynamic" and the build
generates a unique filename for each "compile".

```javacript
import live from "../../content/live.json";
import usamapsvg from "../../../content/usa_w_territories.svg";
```

...

```javascript
<img src={usamapsvg} alt="Map of states using ReportStream"/>
```

outputs to:

```text
build/static/media/usa_w_territories.c74b7be2.svg
```

**Files**

* `content.json`: Page meta data (title, description, etc). Intended to help with localization?
* `live.json`: US State names RS is "live". This file is a bit outdated. It originally was required by the CdcMap
  component, but was also used to generate a 508 compliant list in html. Now it's just used for the html list. Editing
  the map graphics requires editing the .svg file's style section
* `site.json`: site map data used to build urls
* `usa_w_territories.svg`: Map of US with some states styled differently. Edit this file when adding states.
* `README.md`: Obviously this file, but it is NOT included in `/build` output because in is not imported anywhere.
* `getting_started_csv_upload.json`: Content for `/getting-started/testing-facilites`. Includes plain-language documentation for our standard schema.
