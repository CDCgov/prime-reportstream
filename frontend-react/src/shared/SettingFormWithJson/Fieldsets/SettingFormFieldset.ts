import { PropsWithChildren } from "react";

import { reportStreamFilterDefinitionChoices } from "../../../config/endpoints/settings";

export interface SettingFormFieldsetProps extends PropsWithChildren {}

export const filterHint = `Available Filters: ${reportStreamFilterDefinitionChoices.join(
    ", ",
)}`;
