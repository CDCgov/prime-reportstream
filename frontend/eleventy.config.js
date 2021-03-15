// Use same port as the prime-router server API being mocked out
//const PRIME_api = process.env.PRIME_api || 'http://localhost:7071/api';

// Use different port
const PRIME_api = process.env.PRIME_api || 'http://localhost:9000/api';


module.exports = function eleventy_config(cfg) {
  cfg.addFilter('as_literal',
    value => JSON.stringify(value) )

  cfg.addPassthroughCopy('src/js')
  cfg.addPassthroughCopy('src/css')

  _with_live_reloading(cfg)

  return {
    dir: {
      output: './_site',
      input: './src',
    },
  }
}


function _with_live_reloading(cfg) {


  cfg.setBrowserSyncConfig({
    port: 7071,
    // port: 7079,

    // see https://browsersync.io/docs/options
    minify: false,
    online: false, // local dev: don't use xip or tunnel tools
    open: false, // don't open browser windows for us
    cors: true,

   })
}
