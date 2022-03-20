import { Api, ApiConfig, HTTPMethod } from "../api/Api";

export class SampleObject {
    constructor(string: string, bool: boolean, num: number) {
        this.string = string;
        this.bool = bool;
        this.num = num;
    }
    string = "";
    bool = false;
    num = -1;
}

class SampleApi extends Api {
    getSampleList = () => {
        return this.configure<SampleObject[]>({
            method: "GET" as HTTPMethod,
            url: this.basePath,
        });
    };

    postSampleItem = (obj: SampleObject) => {
        return this.configure<SampleObject>({
            method: "POST",
            url: this.basePath,
            data: obj,
        });
    };

    patchSampleItem = (id: number, update: Partial<SampleObject>) => {
        return this.configure<Partial<SampleObject>>({
            method: "PATCH",
            url: `${this.basePath}/${id}`,
            data: update,
        });
    };

    deleteSampleItem = (id: number) => {
        return this.configure<null>({
            method: "DELETE",
            url: `${this.basePath}/${id}`,
        });
    };
}

const config = new ApiConfig({
    root: "http://testhost:0000/api",
    headers: {
        Authorization: "Bearer [token]",
    },
});
export const sampleApi = new SampleApi(config, "sample");
