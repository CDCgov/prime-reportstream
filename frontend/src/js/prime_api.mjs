
export const prime_rest_api = {
  fetch: 'undefined' === typeof window ? undefined
    : window.fetch.bind(window),

  api_url: 'undefined' === typeof window ? undefined
    : new URL('/api/', window.location),

  _api_options: {
      method: 'GET', // *GET, POST, PUT, DELETE, etc.
      mode: 'same-origin', // no-cors, *cors, same-origin
      cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
      credentials: 'same-origin', // include, *same-origin, omit
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
    },

  async json_rest_api(route, method, body) {
    let options = {... this._api_options, method}

    if (undefined !== body)
      options.body = JSON.stringify(body)

    let request = this.fetch(
        new URL(route, this.api_url),
        options)

    let response = await request
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

export default prime_rest_api
