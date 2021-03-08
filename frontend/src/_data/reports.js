const moment = require('moment');
const axios = require('axios')

module.exports = async function() {
    return [
        {
            sent: moment(),
            via: "SFTP",
            positive: 910,
            total: 4507,
            fileType: "CSV",
            type: "Electronic Lab Report",
            reportId: "7205beah-9qhl-6ft2-16e5-238p",
            expires: 7,
            facilities: [
                {
                    organization: "Cyberdyne",
                    facility: "Encino, CA",
                    CLIA: "1125879476",
                    positive: 249,
                    total: 521
                },
                {
                    organization: "Omni Consumer Products",
                    facility: "Detroit, MI",
                    CLIA: "1125879467",
                    positive: 249,
                    total: 521
                },
                {
                    organization: "Weyland Yutani Corp.",
                    facility: "LV-426",
                    CLIA: "1125879400",
                    positive: 12,
                    total: 921
                },
            ],
            actions: [
                {
                    date: moment(),
                    user: "E. Ripley",
                    action: "Viewed file"
                },
                {
                    date: moment().add( 12, "minutes" ),
                    user: "J. Vasquez",
                    action: "Downloaded CSV"
                }
            ]
        },
        {
            sent: moment().add( -1, "days" ),
            via: "SFTP",
            positive: 910,
            total: 4507,
            fileType: "CSV",
            type: "Electronic Lab Report",
            reportId: "7205beah-9qhl-6ft2-16e5-238p",
            expires: 6,
            facilities: [
                {
                    organization: "Cyberdyne",
                    facility: "Encino, CA",
                    CLIA: "1125879476",
                    positive: 2497,
                    total: 521
                }
            ],
            actions: [
                {
                    date: new Date(),
                    user: "E.Ripley",
                    action: "Viewed file"
                }
            ]
        },
        {
            sent: moment().add( -2, "days" ),
            via: "SFTP",
            positive: 910,
            total: 4507,
            fileType: "CSV",
            type: "Electronic Lab Report",
            reportId: "7205beah-9qhl-6ft2-16e5-238p",
            expires: 5,
            facilities: [
                {
                    organization: "Cyberdyne",
                    facility: "Encino, CA",
                    CLIA: "1125879476",
                    positive: 2497,
                    total: 521
                }
            ],
            actions: [
                {
                    date: new Date(),
                    user: "E.Ripley",
                    action: "Viewed file"
                }
            ]
        },     
    ]
}