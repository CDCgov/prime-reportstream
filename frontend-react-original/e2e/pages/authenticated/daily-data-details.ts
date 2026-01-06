import { API_WATERS_REPORT, URL_REPORT_DETAILS } from "./report-details";
import { RSDelivery, RSFacility } from "../../../src/config/endpoints/deliveries";
import { MOCK_GET_DELIVERY } from "../../mocks/delivery";
import { MOCK_GET_FACILITIES } from "../../mocks/facilities";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../BasePage";

const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";

export class DailyDataDetailsPage extends BasePage {
    static readonly API_DELIVERY = `${API_WATERS_REPORT}/${id}/delivery`;
    static readonly API_FACILITIES = `${API_WATERS_REPORT}/${id}/facilities`;
    protected _reportDelivery: RSDelivery;
    protected _facilities: RSFacility[];

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: `${URL_REPORT_DETAILS}/${id}`,
                title: "ReportStream - CDC's free, interoperable data transfer platform",
                heading: testArgs.page.getByRole("heading", {
                    name: "Daily Data Details",
                }),
            },
            testArgs,
        );

        this._reportDelivery = {
            batchReadyAt: "",
            deliveryId: 0,
            expires: "",
            fileName: "",
            fileType: "",
            receiver: "",
            reportId: "",
            reportItemCount: 0,
            topic: "",
        };
        this._facilities = [];
        this.addResponseHandlers([
            [DailyDataDetailsPage.API_DELIVERY, async (res) => (this._reportDelivery = await res.json())],
            [DailyDataDetailsPage.API_FACILITIES, async (res) => (this._facilities = await res.json())],
        ]);
        this.addMockRouteHandlers([this.createMockDeliveryHandler(), this.createMockFacilitiesHandler()]);
    }

    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.testArgs.storageState === this.testArgs.adminLogin.path;
    }

    createMockDeliveryHandler(): RouteHandlerFulfillEntry {
        return [
            DailyDataDetailsPage.API_DELIVERY,
            () => {
                return {
                    json: MOCK_GET_DELIVERY,
                };
            },
        ];
    }

    createMockFacilitiesHandler(): RouteHandlerFulfillEntry {
        return [
            DailyDataDetailsPage.API_FACILITIES,
            () => {
                return {
                    json: MOCK_GET_FACILITIES,
                };
            },
        ];
    }
}
