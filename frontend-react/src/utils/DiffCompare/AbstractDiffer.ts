/**
 * The text differ and the json differ should return the same data type so they can be easily swapped.
 */
export interface DifferMarkupResult {
    left: { normalized: string; markupText: string };
    right: { normalized: string; markupText: string };
}
