import { context, getOctokit } from "@actions/github";

async function run() {
  const { issue_number, github_token, owner, repo, website_link } = process.env

  const octokit = new GitHub(github_token);

  try {
    const { data } = await octokit.issues.createComment({
      owner,
      repo,
      issue_number,
      // We unfortunately need to use replace to get rid
      // of extraneous double quotes
      body: `[Review website here](${website_link.replace(/"/gs, '')})`
    });

    // for debugging, lets log the created comment
    console.log('created comment', data);
  } catch (err) {
    throw err
  }
}

run();