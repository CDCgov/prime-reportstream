const ReportData = [
    {
        sent: new moment().utc(),
        via: 'ELR',
        positive: 1,
        total: 8,
        fileType: 'CSV',
        type: 'CSV',
        reportId: '90a33d88-cd46-11eb-b8bc-0242ac130003',
        expires: moment().add(30, 'days').utc(),
        sendingOrg: 'simple-report',
        receivingOrg: 'localhost-phd',
        receivingOrgSvc: 'OTC',
        facilities: [
            {
                organization: 'localhost-phd',
                facility: 'Eastside Nursing',
                CLIA: '111111',
                positive: 1,
                total: 3
            },
            {
                organization: 'localhost-phd',
                facility: 'Westside Nursing',
                CLIA: '222222',
                positive: 0,
                total: 4
            },
            {
                organization: 'localhost-phd',
                facility: 'Central School',
                CLIA: '333333',
                positive: 0,
                total: 1
            }
        ],
        actions: []
    }, {
        sent: new moment().add(-1, 'day').utc(),
        via: 'ELR',
        positive: 1,
        total: 10,
        fileType: 'CSV',
        type: 'CSV',
        reportId: 'cd8f1bdc-cd4a-11eb-b8bc-0242ac130003',
        expires: moment().add(29, 'days').utc(),
        sendingOrg: 'simple-report',
        receivingOrg: 'localhost-phd',
        receivingOrgSvc: 'OTC',
        facilities: [
            {
                organization: 'localhost-phd',
                facility: 'Eastside Nursing',
                CLIA: '111111',
                positive: 1,
                total: 4
            },
            {
                organization: 'localhost-phd',
                facility: 'Westside Nursing',
                CLIA: '222222',
                positive: 0,
                total: 4
            },
            {
                organization: 'localhost-phd',
                facility: 'Central School',
                CLIA: '333333',
                positive: 0,
                total: 2
            }
        ],
        actions: []
    }, {
        sent: new moment().add(-2, 'day').utc(),
        via: 'ELR',
        positive: 1,
        total: 10,
        fileType: 'CSV',
        type: 'CSV',
        reportId: 'd6b3d48c-cd4a-11eb-b8bc-0242ac130003',
        expires: moment().add(28, 'days').utc(),
        sendingOrg: 'simple-report',
        receivingOrg: 'localhost-phd',
        receivingOrgSvc: 'OTC',
        facilities: [
            {
                organization: 'localhost-phd',
                facility: 'Eastside Nursing',
                CLIA: '111111',
                positive: 1,
                total: 3
            },
            {
                organization: 'localhost-phd',
                facility: 'Westside Nursing',
                CLIA: '222222',
                positive: 0,
                total: 5
            },
            {
                organization: 'localhost-phd',
                facility: 'Central School',
                CLIA: '333333',
                positive: 0,
                total: 2
            }
        ],
        actions: []
    }, {
        sent: new moment().add(-3, 'day').utc(),
        via: 'ELR',
        positive: 1,
        total: 8,
        fileType: 'CSV',
        type: 'CSV',
        reportId: 'e0ebbf96-cd4a-11eb-b8bc-0242ac130003',
        expires: moment().add(27, 'days').utc(),
        sendingOrg: 'simple-report',
        receivingOrg: 'localhost-phd',
        receivingOrgSvc: 'OTC',
        facilities: [
            {
                organization: 'localhost-phd',
                facility: 'Westside Nursing',
                CLIA: '222222',
                positive: 0,
                total: 4
            },
            {
                organization: 'localhost-phd',
                facility: 'Central School',
                CLIA: '333333',
                positive: 0,
                total: 4
            }
        ],
        actions: []
    }, {
        sent: new moment().add(-4, 'day').utc(),
        via: 'ELR',
        positive: 1,
        total: 11,
        fileType: 'CSV',
        type: 'CSV',
        reportId: 'ea54d9aa-cd4a-11eb-b8bc-0242ac130003',
        expires: moment().add(26, 'days').utc(),
        sendingOrg: 'simple-report',
        receivingOrg: 'localhost-phd',
        receivingOrgSvc: 'OTC',
        facilities: [
            {
                organization: 'localhost-phd',
                facility: 'Eastside Nursing',
                CLIA: '111111',
                positive: 1,
                total: 3
            },
            {
                organization: 'localhost-phd',
                facility: 'Westside Nursing',
                CLIA: '222222',
                positive: 0,
                total: 4
            },
            {
                organization: 'localhost-phd',
                facility: 'Central School',
                CLIA: '333333',
                positive: 0,
                total: 4
            }
        ],
        actions: []
    }, {
        sent: new moment().add(-5, 'day').utc(),
        via: 'ELR',
        positive: 1,
        total: 5,
        fileType: 'CSV',
        type: 'CSV',
        reportId: 'f361f802-cd4a-11eb-b8bc-0242ac130003',
        expires: moment().add(25, 'days').utc(),
        sendingOrg: 'simple-report',
        receivingOrg: 'localhost-phd',
        receivingOrgSvc: 'OTC',
        facilities: [
            {
                organization: 'localhost-phd',
                facility: 'Westside Nursing',
                CLIA: '222222',
                positive: 0,
                total: 4
            },
            {
                organization: 'localhost-phd',
                facility: 'Central School',
                CLIA: '333333',
                positive: 0,
                total: 1
            }
        ],
        actions: []
    }, {
        sent: new moment().add(-6, 'day').utc(),
        via: 'ELR',
        positive: 1,
        total: 8,
        fileType: 'CSV',
        type: 'CSV',
        reportId: 'fc9f7e9e-cd4a-11eb-b8bc-0242ac130003',
        expires: moment().add(24, 'days').utc(),
        sendingOrg: 'simple-report',
        receivingOrg: 'localhost-phd',
        receivingOrgSvc: 'OTC',
        facilities: [
            {
                organization: 'localhost-phd',
                facility: 'Eastside Nursing',
                CLIA: '111111',
                positive: 1,
                total: 3
            },
            {
                organization: 'localhost-phd',
                facility: 'Westside Nursing',
                CLIA: '222222',
                positive: 0,
                total: 4
            },
            {
                organization: 'localhost-phd',
                facility: 'Central School',
                CLIA: '333333',
                positive: 0,
                total: 1
            }
        ],
        actions: []
    }
]