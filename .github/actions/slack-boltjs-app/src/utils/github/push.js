import request from "./request.js";
import https from "https";

const push = async ({ localStorage, args, api, respond, say, force, isCommand }) => {
  branchPush(localStorage, args, api, force, respond, say, isCommand);
};

export default push;

// Define a function to check the configuration for branch push
const branchPushCheckConfiguration = function (
  localStorage,
  sourceBranch,
  targetBranch,
  app,
  token,
  respond
) {
  // Get the deploy targets from the environment variable
  const deployTargets = process.env.GITHUB_TARGET_BRANCHES.split(",");

  // Define an array of error messages and conditions
  const errors = [
    {
      message: "Missing configuration: GITHUB_TOKEN",
      condition: !token,
    },
    {
      message:
        "Missing configuration: [for :owner/:repo] or GITHUB_OWNER/GITHUB_REPO",
      condition: !app,
    },
    {
      message:
        "Missing <sourceBranch>: gh-deploy <sourceBranch> to <targetBranch>",
      condition: !sourceBranch,
    },
    {
      message:
        "Missing <targetBranch>: gh-deploy <sourceBranch> to <targetBranch>",
      condition: !targetBranch,
    },
    {
      message: "Missing configuration: GITHUB_TARGET_BRANCHES",
      condition: !process.env.GITHUB_TARGET_BRANCHES,
    },
    {
      message: `\"${targetBranch}\" is not in available target branches. Use:  <@bot name> gh-targets`,
      condition: !deployTargets.includes(targetBranch),
    },
  ];

  // Loop through the errors and respond if any condition is true
  for (const error of errors) {
    if (error.condition) {
      respond(error.message);
      return false;
    }
  }

  if (localStorage.getItem(targetBranch)) {
    respond(`Branch ${targetBranch} is locked by <@${localStorage.getItem(targetBranch)}>`);
    return false;
  }

  // Return true if no errors are found
  return true;
};

// Define constants
const token = process.env.GITHUB_TOKEN;
const app = process.env.GITHUB_REPO;
const port = 443;
const method = "PATCH";
const headers = {
  "User-Agent": "request",
  "X-GitHub-Api-Version": "2022-11-28",
};

// Define a function to get the SHA of a branch
const getSHA = (api, branch) => {
  return new Promise((resolve, reject) => {
    // Set the options for the HTTPS request
    const options = {
      hostname: api,
      port,
      path: `/repos/${app}/git/refs/heads/${branch}`,
      method: "GET",
      headers,
    };

    // Make the HTTPS request
    const req = https.request(options, (res) => {
      let data = "";

      // Concatenate the data chunks
      res.on("data", (chunk) => {
        data += chunk;
      });

      // Parse the JSON response and resolve the promise with the SHA
      res.on("end", () => {
        try {
          const sha = JSON.parse(data).object.sha;
          resolve(sha);
        } catch (error) {
          reject(error);
        }
      });
    });

    // Handle errors and end the request
    req.on("error", (error) => {
      reject(error);
    });
    req.end();
  });
};

// Define a function to push a branch to another branch
const branchPush = async (localStorage, args, api, force, respond, say, isCommand) => {
  // Get the source and target branches from the arguments
  const sourceBranch = isCommand ? args[0] : args[2];
  const targetBranch = isCommand ? args[2] : args[4];

  if (
    !branchPushCheckConfiguration(
      localStorage,
      sourceBranch,
      targetBranch,
      app,
      token,
      respond
    )
  ) {
    return;
  }

  // Get the SHA of the source branch
  try {
    const sha = await getSHA(api, sourceBranch);

    // Prepare the data for the push request
    const data = JSON.stringify({
      sha,
      force,
    });

    // Set the path for the push request
    const path = `/repos/${app}/git/refs/heads/${targetBranch}`;

    // Make the push request using the request module
    const out = await request({
      api,
      path,
      method,
      token,
      data,
      say,
      msg: "",
    });

    // If the push request is successful, respond and say accordingly
    if (out) {
      const json = JSON.parse(out);
      console.log(json);
      respond(
        `${
          force ? "Force pushed" : "Pushed"
        } commit \"${sha}\" to branch \"${targetBranch}\"`
      );
      say(
        `\`deploy ${sourceBranch} to ${targetBranch} for ${app}\` triggered! :rocket:`
      );
    }
  } catch (error) {
    // If there is an error, say that the branch was not found
    say(`I failed to find branch \"${sourceBranch}\"!`);
  }

  return true;
};
