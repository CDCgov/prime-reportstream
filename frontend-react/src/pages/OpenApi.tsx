import { RedocStandalone } from 'redoc';

import openapiDoc from '../../../prime-router/docs/api/openapi.yaml?url'

function OpenApiPage() {
    return <RedocStandalone specUrl={openapiDoc} />
}

export default OpenApiPage