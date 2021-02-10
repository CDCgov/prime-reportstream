const env_vars = {
  OKTA_redirect: process.env.OKTA_redirect || 'http://localhost:7071/api/download',
  OKTA_clientId: process.env.OKTA_clientId || '0oa6fm8j4G1xfrthd4h6',
  OKTA_baseUrl: process.env.OKTA_baseUrl || 'hhs-prime.okta.com',
}

// use JSON.parse to validate content
for (let [key, value] of Object.entries(env_vars)) {
  if (value != JSON.parse(`"${value}"`)) {
    throw new Error(`Invalid value for process.env.${key}`)
  }
}

module.exports = env_vars
