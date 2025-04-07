import https from "https";

const request = async ({ api, path, method, token, data, say, msg }) => {
  const json = await httpsRequest(api, path, method, token, data, say, msg);
  return json;
};

export default request;

const httpsRequest = (api, path, method, token, data, say, msg) => {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: api,
      port: 443,
      path,
      method,
      headers: {
        "User-Agent": "request",
        Authorization: `token ${token}`,
        "Content-Type": "application/json",
        "X-GitHub-Api-Version": "2022-11-28",
      },
    };
    const req = https.request(options, async (res) => {
      let body = "";
      res.on("data", (chunk) => {
        body += chunk;
      });
      res.on("end", async () => {
        resolve(body);
        if (msg) {
          await say(msg);
        }
      });
    });
    req.on("error", (err) => {
      reject(err);
    });
    if (data) {
      req.write(data);
    }
    req.end();
  });
};
