import boltjs from "@slack/bolt";
const { App, directMention } = boltjs;
import fs from "fs";
import ghRouter from "./utils/github/router.js";
import appHome from "./views/app_home.js";

const app = new App({
  token: process.env.SLACK_BOT_TOKEN,
  appToken: process.env.SLACK_APP_TOKEN,
  socketMode: true,
});

// Use a single message listener with a switch statement to handle different commands
app.message(directMention(), async ({ message, say }) => {
  const command = message.text.split(" ")[1].split("-")[0];
  switch (command) {
    case ":wave:":
      await say(`Hello, <@${message.user}>`);
      break;
    case "gh":
      await ghRouter({ message, say });
      break;
    case "help":
      const filename = ".help";
      const data = fs.readFileSync(filename, "utf8");
      say(data);
      break;
    default:
      // Handle unknown commands
      say(
        `Sorry, I don't recognize that command. Try typing \`@bot help\` for more information.`
      );
  }
});

// Listen for users opening App Home
app.event("app_home_opened", async ({ event, client }) => {
  appHome({ event, client });
});

(async () => {
  const port = process.env.PORT || 3000;

  // Start your app
  await app.start(port);
  console.log(`⚡️ Slack Bolt app is running on port ${port}!`);
})();
