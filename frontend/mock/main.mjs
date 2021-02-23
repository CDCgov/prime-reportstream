import {
  openapi_basic,
  new_jwt_token,
  openapi_jwtAuth,
  openapi_server,
} from './_api_utils.mjs'

//const port = 7071 // Use same port as the prime-router server API being mocked out
const port = 9000 // Use different port

let api = openapi_basic(
  './mock/demo.openapi.yml',
  // './mock/prime.openapi.yml',
  )

console.info('')
console.info(`export MOCK_URL='http://localhost:${port}'`)
console.info(`export MOCK_ACCEPT='Accept: application/json'`)

if (0) {
  api = openapi_jwtAuth(api)
  console.info(`export MOCK_AUTH='Authorization: Bearer ${new_jwt_token()}'`)
}

let app = openapi_server(api)

app.listen(port, () => {
  console.info('\n')
  console.info(`# Mock API is ready: http://localhost:${port}`)
  console.info('')
  console.info('# invoke GET /me endpoint')
  console.info(`curl -H $MOCK_ACCEPT -H $MOCK_AUTH $MOCK_URL/me`)
  console.info('')
  console.info('# invoke GET /_mock_login_ to get new JSON Web Token (JWT) token')
  console.info(`curl -H $MOCK_ACCEPT -H $MOCK_AUTH -X POST $MOCK_URL/_mock_login_`)
  console.info('')
})
