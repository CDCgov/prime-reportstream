
export const prime_rest_api = {
  _api_options: {
      mode: 'same-origin',
      cache: 'no-cache',
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
    },

  rest_api_fetch(route, method='GET', body) {
    let options = {... this._api_options, method}

    if (undefined !== body)
      options.body = JSON.stringify(body)

    return this.fetch(
      new URL(route, this.api_url),
      options)
  },

  async json_rest_api(route, method, body) {
    let response = await this.rest_api_fetch(route, method, body)
    return await response.json()
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
}

if ('undefined' !== typeof window) {
  prime_rest_api.fetch = window.fetch.bind(window)
  prime_rest_api.api_url = new URL('/api/', window.location)
}

export default prime_rest_api
