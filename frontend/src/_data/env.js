const env = {
  OKTA_redirect: process.env.OKTA_redirect || 'http://localhost:7071/api/download',
  // OKTA_redirect: process.env.OKTA_redirect || 'http://localhost:7079/authorize.html',
  OKTA_clientId: process.env.OKTA_clientId || '0oa6fm8j4G1xfrthd4h6',
  OKTA_baseUrl: process.env.OKTA_baseUrl || 'hhs-prime.okta.com',

  // The PRIME_api is handled by http proxy of `/api`, using same-site policy to avoid CORS hurdles.
  // See `frontend/eleventy.config.js` and `frontend/mock/` for details.
  // PRIME_api: process.env.PRIME_api || 'http://localhost:7071/api',
}

// use JSON.parse to validate content
for (let [key, value] of Object.entries(env)) {
  if (value != JSON.parse(`"${value}"`)) {
    throw new Error(`Invalid value for process.env.${key}`)
  }
}

module.exports = env ;
