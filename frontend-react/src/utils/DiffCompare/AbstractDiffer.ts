/**
 * The text differ and the json differ should have the same interface so they can be easily swapped.
 */
export type DifferMarkupResult = {
    left: { normalized: string; markupText: string };
    right: { normalized: string; markupText: string };
};

export interface DifferMarkup {
    (leftText: string, rightText: string): DifferMarkupResult;
}
