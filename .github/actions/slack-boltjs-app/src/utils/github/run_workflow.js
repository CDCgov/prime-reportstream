import defaultBranch from "../../utils/github/default_branch.js";
import request from "../../utils/github/request.js";

const run_workflow = async ({ args, api, say, inputs }) => {
  const [workflowFile, app = process.env.GITHUB_REPO, branch] = args.slice(2);
  const token = process.env.GITHUB_TOKEN;
  // Use an object to store the request data
  let data = { ref: branch || (await defaultBranch({ api, app, token, say })) };

  if (inputs) {
    // Use JSON.parse and JSON.stringify to convert the inputs to a valid JSON object
    data.inputs = JSON.parse(
      `{${inputs}}`
        .replace(/([,\{] *)(\w+):/g, '$1"$2":')
        .replace(
          /([,\{] *"\w+":)(?! *-?[0-9\.]+[,\}])(?! *[\{\[])( *)([^,\}]*)/g,
          '$1$2"$3"'
        )
    );
  }

  const stringData = JSON.stringify(data);
  const path = `/repos/${app}/actions/workflows/${workflowFile}/dispatches`;
  const method = "POST";

  const out = await request({
    api,
    path,
    method,
    token,
    data: stringData,
    say,
  });
  if (out) {
    say(JSON.parse(out).message);
  } else {
    say(
      `Triggered workflow \`${workflowFile}\` with \`${stringData}\` for \`${app}\`! :rocket:`
    );
  }
};

export default run_workflow;
