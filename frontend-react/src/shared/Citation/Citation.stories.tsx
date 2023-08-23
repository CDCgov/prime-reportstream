// AutoUpdateFileChromatic
import React from "react";

import { Citation } from "./Citation";

export default {
    title: "components/Citation",
    component: Citation,
};

const citation = {
    title: "Connecting to more states with less work",
    quote: "The selling point for me was I wanted to expand into other states so I was working with several different states for different credentialing and [ReportStream] had connections already to most of those states.",
    author: "Desiree Bock",
    authorTitle: "Senior VP of Operations at Dobrin Group",
};

export const RSCitation = (): React.ReactElement => <Citation {...citation} />;
