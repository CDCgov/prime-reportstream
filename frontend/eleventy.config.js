// Use same port as the prime-router server API being mocked out
//const PRIME_api = process.env.PRIME_api || 'http://localhost:7071/api';

// Use different port
const PRIME_api = process.env.PRIME_api || 'http://localhost:9000/api';


module.exports = function eleventy_config(cfg) {
  cfg.addFilter('as_literal',
    value => JSON.stringify(value) )

  cfg.addPassthroughCopy('src/js')
  cfg.addPassthroughCopy('src/css')

  if ('production' != process.env.NODE_ENV) {
    _with_live_reloading(cfg)
  }

  return {
    dir: {
      output: './_site',
      input: './src',
    },
  }
}


function _with_live_reloading(cfg) {
  const { createProxyMiddleware } = require('http-proxy-middleware')

  cfg.setBrowserSyncConfig({
    port: 7071,
    // port: 7079,

    // see https://browsersync.io/docs/options
    minify: false,
    online: false, // local dev: don't use xip or tunnel tools
    open: false, // don't open browser windows for us
    cors: true,

    // see https://browsersync.io/docs/options#option-middleware
    middleware: [
      { route: `/api/download`,
        handle(req, res, next) {
          // Hack: the registered OKTA_redirect is `/api/download` -- rewrite it!
          res.writeHead(302, {location: '../authorize.html'})
          res.end()
        }
      },

      // The PRIME_api is handled by http proxy of `/api`, using same-site policy to avoid CORS hurdles.
      // See `frontend/mock/` and `frontend/src/_data/env.js` for additional details.
      // Proxy `/api` to prime-router or mock server backend
      { route: `/api`, handle: createProxyMiddleware(PRIME_api) },
    ],
  })
}
