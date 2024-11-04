module.exports = async (page) => {
    page.on('request', (request) => {
      console.log(`Request: ${request.method()} ${request.url()}`);
    });
  
    page.on('response', (response) => {
      console.log(`Response: ${response.status()} ${response.url()}`);
    });
  };
  