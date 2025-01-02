import request from "./request.js";

const default_branch = async function ({ api, app, token, say, msg }) {
  const path = `/repos/${app}`;
  const method = "GET";
  const data = null;
  const out = await request({ api, path, method, token, data, say, msg });
  return JSON.parse(out)?.default_branch;
};

export default default_branch;
