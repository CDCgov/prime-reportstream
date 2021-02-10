const { createProxyMiddleware } = require('http-proxy-middleware')

const PRIME_api = process.env.PRIME_api || 'http://localhost:7071/api';


module.exports = function eleventy_config(cfg) {
  cfg.setBrowserSyncConfig({
    // see https://browsersync.io/docs/options
    minify: false,
    online: false, // local dev: don't use xip or tunnel tools
    open: false, // don't open browser windows for us

    // see https://browsersync.io/docs/options#option-middleware
    middleware: [
      { route: `/api`, handle: createProxyMiddleware(PRIME_api) },
    ],
  })

  cfg.addPassthroughCopy('src/css')

  return {
    dir: {
      output: './_site',
      input: './src',
    },
  }
}
