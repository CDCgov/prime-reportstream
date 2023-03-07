import { BasePageComponent } from "./Base";

export class DateRangeFilter extends BasePageComponent {
    elementsMap = {
        // TODO: these elements share the same testid
        startDateInput: "[aria-describedby='start-date-label']",
        endDateInput: "[aria-describedby='end-date-label']",
        // TODO: collisions on this generic testid
        filterButton: "[data-testid='button']",
        clearbutton: "[data-testid='button'][name='clear-button']",
    };
}

export default new DateRangeFilter();
