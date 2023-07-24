window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle({
    // url: "api.yaml",
    // using urls instead of url is important
    // with urls: the paths in the array are not evaluated,
    // for details, refer to: https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/
    // Note, evaluation of a url is vulnerable to javascript injection,
    urls: [
        {
            url: "api.yaml",
            name: "api.yaml"
        },
    ],
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });

  //</editor-fold>
};
