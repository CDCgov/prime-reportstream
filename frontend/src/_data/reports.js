const axios = require('axios')
const moment = require('moment')

module.exports = async function() {

    /*
    let config = {
        headers: {'Access-Control-Allow-Origin': '*'}
        }
        
        const response = await axios.get('http://localhost:7071/api/history/report', config);
        console.log( response.data );
        return response.data;
};*/
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
            total: 4790,
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
            total: 4534,
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
        {
            sent: moment().add( -3, "days" ),
            via: "SFTP",
            positive: 910,
            total: 4103,
            fileType: "CSV",
            type: "Electronic Lab Report",
            reportId: "7205beah-9qhl-6ft2-16e5-238p",
            expires: 4,
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
            sent: moment().add( -4, "days" ),
            via: "SFTP",
            positive: 910,
            total: 4342,
            fileType: "CSV",
            type: "Electronic Lab Report",
            reportId: "7205beah-9qhl-6ft2-16e5-238p",
            expires: 3,
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
            sent: moment().add( -5, "days" ),
            via: "SFTP",
            positive: 910,
            total: 4789,
            fileType: "CSV",
            type: "Electronic Lab Report",
            reportId: "7205beah-9qhl-6ft2-16e5-238p",
            expires: 2,
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
        },        {
            sent: moment().add( -6, "days" ),
            via: "SFTP",
            positive: 910,
            total: 4499,
            fileType: "CSV",
            type: "Electronic Lab Report",
            reportId: "7205beah-9qhl-6ft2-16e5-238p",
            expires: 1,
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
