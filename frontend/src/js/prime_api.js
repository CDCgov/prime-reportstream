
export const prime_rest_api = {
  APIError: class PRIME_API_Error extends Error {},

  _api_options: {
      mode: 'same-origin',
      cache: 'no-cache',
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
    },

  rest_api_fetch(route, method='GET', body=undefined) {
    let options = {... this._api_options, method}

    if (undefined !== body)
      options.body = JSON.stringify(body)

    return this.fetch(
      new URL(route, this.api_url),
      options)
  },

  async json_rest_api(route, method, body) {
    let resp = await this.rest_api_fetch(route, method, body)
    if (!resp.ok) {
      let {status, statusText} = resp
      let err = new this.APIError(`${status} ${statusText}`)
      err.status = status
      err.statusText = statusText
      if (status < 500) {
        err.detail = resp.json()
      }
      throw err
    }
    return resp.json()
  },

  async json_get(route) {
    return this.json_rest_api(route, 'GET')
  },

  async json_post(route, body) {
    return this.json_rest_api(route, 'POST', body)
  },

  async json_put(route, body) {
    return this.json_rest_api(route, 'PUT', body)
  },

  async jwt_login(jwt, td_expiration=5*60000) {
    jwt = await jwt

    // set JSON Web Token (JWT) cookie with 5 minute expiration
    let ts_expires = new Date(Date.now() + td_expiration)
    document.cookie = `jwt=${jwt};expires=${ts_expires.toUTCString()};path=/`; 

    // Save raw JWT for direct API calls 
    const stg = window.localStorage
    stg.setItem('jwt', jwt)
    stg.setItem('jwt_expires', ts_expires.toISOString())

    // base64 decode JWT body part using Fetch, DataURL, and JSON
    let jwt_body = await window.fetch(`data:application/json;base64,${jwt.split('.')[1]}`)
    jwt_body = await jwt_body.json()

    let {given_name, family_name, sub} = jwt_body || {}
    jwt_body = {given_name, family_name, sub}

    // Save decoded JWT body for local display
    stg.setItem('jwt_body', JSON.stringify(jwt_body))
    return jwt_body
  }
}

if ('undefined' !== typeof window) {
  prime_rest_api.fetch = window.fetch.bind(window)
  prime_rest_api.api_url = new URL('/api/', window.location)
}

export default prime_rest_api
