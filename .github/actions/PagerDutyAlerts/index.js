const apiUrl = 'https://api.pagerduty.com/schedules';
const outputElement = document.getElementById('output');

fetch(apiUrl)
  .then(response => {
    if (!response.ok) {
      throw new Error('Network response was not ok');
    }
    return response.json();
  })
  .then(data => {
    // Display data in an HTML element
    outputElement.textContent = JSON.stringify(data, null, 2);
  })
  .catch(error => {
    console.error('Error:', error);
  });





  function api(apiParameters) {
    var _a;
    // If the apiParameters don't include `endpoint` treat it as a partial
    // application.
    
    const types = {
        bearer: 'Bearer ',
        token: 'Token token=',
    };
    const { endpoint, server = 'api.pagerduty.com', token, tokenType = apiParameters.tokenType || 'token', url, version = 2, data, ...rest } = apiParameters;
    const config = {
        method: 'GET',
        ...rest,
        headers: {
            Accept: `application/vnd.pagerduty+json;version=${version}`,
            Authorization: `${types[tokenType]}${token}`,
            ...rest.headers,
        },
    };
    // Allow `data` for `queryParameters` for requests without bodies.
    if (isReadonlyRequest(config.method) && data) {
        config.queryParameters =
            (_a = config.queryParameters) !== null && _a !== void 0 ? _a : data;
    }
    else {
        config.body = JSON.stringify(data);
    }
    return apiRequest(url !== null && url !== void 0 ? url : `https://${server}/${endpoint.replace(/^\/+/, '')}`, config);
}