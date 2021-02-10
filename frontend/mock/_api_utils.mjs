import express from 'express'
import morgan from 'morgan'
import jwt from 'jsonwebtoken'

import {OpenAPIBackend} from 'openapi-backend'



// Mock server using openapi-backend with an OpenAPI YAML spec file

export function openapi_basic(openapi_filename='./mock/prime.openapi.yml') {
  const api = new OpenAPIBackend({
    definition: openapi_filename,
    handlers: {
      async validationFail(oapi_ctx, req, res) {
        res.status(400)
          .json({ err: oapi_ctx.validation.errors })
      },
      async notFound(oapi_ctx, req, res) {
        res.status(404)
          .json({ err: 'not found' })
      },
      async notImplemented(oapi_ctx, req, res) {
        const { status, mock } =
          oapi_ctx.api.mockResponseForOperation(
              oapi_ctx.operation.operationId)

        return res
          .status(status)
          .json(mock)
      },
    },
  })
  api.init()

  // a tautology... make sure the code thinks so too!
  return assure_openapi_backend(api)
}

function assure_openapi_backend(api) {
  if (!(api instanceof OpenAPIBackend))
    throw new TypeError('Expected api as an OpenAPIBackend instance')

  return api
}




// JSON Web Token (JWT) integration into openapi-backend 

const _jwt_mock_secret_ = 'jwt_mock_secret'
const default_login_obj = { name: 'John Doe', email: 'john@example.com' }
export const new_jwt_token = (login_obj) =>
  jwt.sign(login_obj || default_login_obj, _jwt_mock_secret_)


export function openapi_jwtAuth(api) {

  api.register({
    async unauthorizedHandler(oapi_ctx, req, res) {
      res.status(401)
        .json({ err: 'unauthorized' })
    }
  })

  api.registerSecurityHandler('jwtAuth', validate_jwt_auth_header)
  return api

  async function validate_jwt_auth_header(oapi_ctx, req, res) {
    const authHeader = oapi_ctx.request.headers['authorization']
    if (!authHeader) {
      throw new Error('Missing authorization header')
    }

    const jwt_token = authHeader.replace('Bearer ', '')
    return jwt.verify(jwt_token, _jwt_mock_secret_)
  }
}


// Rapid prototyping server using express and openapi-backend

export function openapi_server(api, cfg={}) {
  assure_openapi_backend(api)

  const app = express()

  // Use applicaiton/json mimetype
  app.use(express.json())

  // logging
  app.use(morgan('combined'))

  // add a JWT supporting mock login
  app.use('/_mock_login_',
    (req, res) => res.status(200)
      .json({token: new_jwt_token()}) )

  // use openapi-backend api as express middleware
  app.use((req, res) =>
    api.handleRequest(req, req, res))

  return app
}
