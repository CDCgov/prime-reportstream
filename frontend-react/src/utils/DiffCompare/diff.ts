/* eslint-disable camelcase */

/**
 * The algorithm implemented here is based on "An O(NP) Sequence Comparison Algorithm"
 * by described by Sun Wu, Udi Manber and Gene Myers
 * Adapted from: https://github.com/cubicdaiya/onp
 */

export enum SES_TYPE {
    DELETE = -1,
    COMMON = 0,
    ADD = 1,
}

export const Diff = (a_: string, b_: string) => {
    type PEntryType = {
        x: number;
        y: number;
        k: number;
    };

    type SesEntryType = {
        sestype: SES_TYPE;
        index: number;
        len: number;
    };

    let a = a_,
        b = b_,
        alen = a.length,
        blen = b.length,
        reverse: boolean = false,
        editdistance: number | null = null,
        offset = alen + 1,
        path: number[] = [],
        pathposi: PEntryType[] = [],
        ses: SesEntryType[] = [],
        sesstate = SES_TYPE.COMMON;
    let lcs = "";

    const init = () => {
        if (alen >= blen) {
            // swap around a=b and b=a if a is longer
            [a, alen, b, blen] = [b, blen, a, alen];

            reverse = true;
            offset = alen + 1;
        }
    };

    const P = (x: number, y: number, k: number): PEntryType => {
        return { x, y, k };
    };

    const addSesElem = (char: string, index: number, sestype: SES_TYPE) => {
        const addnew =
            ses.length === 0 || // first elem special case
            sesstate !== sestype; // we switched, probably back to common.

        sesstate = sestype; // save state to check for next character

        if (addnew) {
            // don't track common
            ses.push({
                sestype,
                index,
                len: 1,
            });
        } else {
            ses[ses.length - 1].len++;
        }
    };

    const snake = (k: number, p: number, pp: number): number => {
        let r, x, y;
        if (p > pp) {
            r = path[k - 1 + offset];
        } else {
            r = path[k + 1 + offset];
        }

        y = Math.max(p, pp);
        x = y - k;
        while (x < alen && y < blen && a[x] === b[y]) {
            ++x;
            ++y;
        }

        path[k + offset] = pathposi.length;
        pathposi.push(P(x, y, r));
        return y;
    };

    const recordseq = (epc: PEntryType[]) => {
        let px_idx = 0,
            py_idx = 0;
        let x_idx = 1,
            y_idx = 1;
        for (let i = epc.length - 1; i >= 0; --i) {
            while (px_idx < epc[i].x || py_idx < epc[i].y) {
                if (epc[i].y - epc[i].x > py_idx - px_idx) {
                    if (reverse) {
                        addSesElem(b[py_idx], y_idx, SES_TYPE.DELETE);
                    } else {
                        addSesElem(b[py_idx], y_idx, SES_TYPE.ADD);
                    }
                    ++y_idx;
                    ++py_idx;
                } else if (epc[i].y - epc[i].x < py_idx - px_idx) {
                    if (reverse) {
                        addSesElem(a[px_idx], x_idx, SES_TYPE.ADD);
                    } else {
                        addSesElem(a[px_idx], x_idx, SES_TYPE.DELETE);
                    }
                    ++x_idx;
                    ++px_idx;
                } else {
                    addSesElem(a[px_idx], x_idx, SES_TYPE.COMMON);
                    lcs += a[px_idx];
                    ++x_idx;
                    ++y_idx;
                    ++px_idx;
                    ++py_idx;
                }
            }
        }
    };

    init();

    return {
        editdistance: function () {
            return editdistance;
        },
        getlcs: function () {
            return lcs;
        },
        getses: function () {
            return ses;
        },
        compose: function () {
            let delta = blen - alen;
            const fp: number[] = []; // js supports sparse arrays so by adding ` || -1` so undefined defaults to -1

            if (blen === 0 || alen === 0) {
                return; // don't try to process empty strings.
            }

            let p = -1;
            do {
                ++p;
                for (let k = -p; k <= delta - 1; ++k) {
                    fp[k + offset] = snake(
                        k,
                        (fp[k - 1 + offset] || -1) + 1,
                        fp[k + 1 + offset] || -1
                    );
                }
                for (let k = delta + p; k >= delta + 1; --k) {
                    fp[k + offset] = snake(
                        k,
                        (fp[k - 1 + offset] || -1) + 1,
                        fp[k + 1 + offset] || -1
                    );
                }
                fp[delta + offset] = snake(
                    delta,
                    (fp[delta - 1 + offset] || -1) + 1,
                    fp[delta + 1 + offset] || -1
                );
            } while ((fp[delta + offset] || -1) !== blen);

            editdistance = delta + 2 * p;

            // turn the fpath into a list of deltas
            let r: number = path[delta + offset];
            const epc: PEntryType[] = [];
            while (r !== null && r >= 0) {
                epc.push(P(pathposi[r].x, pathposi[r].y, pathposi[r].k));
                r = pathposi[r].k;
            }
            recordseq(epc);
        },
    };
};
