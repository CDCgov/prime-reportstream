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
const default_login_obj = {
  sub: 'jane@example.com',
  given_name: 'Jane',
  family_name: 'Doe',
}
export const new_jwt_token = (login_obj) =>
  jwt.sign(login_obj || default_login_obj, _jwt_mock_secret_)


export function openapi_jwt_auth(api, bypass_verify) {

  api.register({
    async unauthorizedHandler(oapi_ctx, req, res) {
      res.status(401)
        .json({ err: 'unauthorized' })
    }
  })

  api.registerSecurityHandler('jwt_bearer_auth', validate_jwt_bearer_auth)
  api.registerSecurityHandler('jwt_cookie_auth', validate_jwt_cookie_auth)

  return api

  async function validate_jwt_bearer_auth(oapi_ctx, req, res) {
    const authHeader = oapi_ctx.request.headers['authorization']
    if (!authHeader) {
      throw new Error('Missing JWT authorization header')
    }

    if (bypass_verify) return true

    // pull jwt token from bearer value
    const jwt_token = authHeader.replace('Bearer ', '')
    return jwt.verify(jwt_token, _jwt_mock_secret_)
  }

  async function validate_jwt_cookie_auth(oapi_ctx, req, res) {
    const cookie = oapi_ctx.request.headers['cookie'] || ''

    const jwt_cookie = cookie.match(/\bjwt\b=\s*([^;]+)/)
    if (!jwt_cookie) {
      throw new Error('Missing JWT authorization cookie')
    }

    if (bypass_verify) return true

    // pull jwt token from first match group
    const jwt_token = jwt_cookie[1]
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
  const mock_login_fns = {
    token(req, res) {
      console.log("TOKEN")
      res.status(200)
        .end(`Authorization: Bearer ${new_jwt_token()}`)
    },

    cookie(req, res) {
      console.log("COOKIE")
      res.status(200)
        .end(`Cookie: jwt=${new_jwt_token()};`)
    },

    json(req, res) {
      console.log("JSON")
      let jwt = new_jwt_token()
      res.status(200)
        .json({token: jwt, cookie: `jwt=${jwt};`})
    },
  }

  for (let api_prefix of ['', '/api']) {
    app.use(`${api_prefix}/_mock_login_token_`, mock_login_fns.token)
    app.use(`${api_prefix}/_mock_login_cookie_`, mock_login_fns.cookie)
    app.use(`${api_prefix}/_mock_login_`, mock_login_fns.json)
  }

  // use openapi-backend api as express middleware
  app.use((req, res) =>
    api.handleRequest(req, req, res))

  return app
}
