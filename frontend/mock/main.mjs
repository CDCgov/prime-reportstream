import {
  openapi_basic,
  openapi_jwt_auth,
  openapi_server,
} from './_api_utils.mjs'

//const port = 7071 // Use same port as the prime-router server API being mocked out
const port = 9000 // Use different port

let api = openapi_basic(
  // './mock/demo.openapi.yml',
  './mock/prime.openapi.yml',
  )

console.info('')
console.info(`export MOCK_URL='http://localhost:${port}'`)
console.info(`export MOCK_ACCEPT='Accept: application/json'`)

if (1) {
  api = openapi_jwt_auth(api, true)
  console.info(`export MOCK_AUTH=$(curl -s $MOCK_URL/_mock_login_token_)`)
  console.info(`export MOCK_COOKIE=$(curl -s $MOCK_URL/_mock_login_cookie_)`)
}

let app = openapi_server(api)

app.listen(port, () => {
  console.info('\n')
  console.info(`# Mock API is ready: http://localhost:${port}`)
  console.info('')
  console.info('# invoke GET /_mock_login_ to get new JSON Web Token (JWT) token')
  console.info(`curl -s $MOCK_URL/_mock_login_token_`)
  console.info(`curl -s $MOCK_URL/_mock_login_cookie_`)
  console.info('')
  console.info('# invoke GET /api/me endpoint')
  console.info(`curl -H $MOCK_AUTH -H $MOCK_ACCEPT $MOCK_URL/api/me`)
  console.info(`curl -H $MOCK_COOKIE -H $MOCK_ACCEPT $MOCK_URL/api/me`)
  console.info('')
})
