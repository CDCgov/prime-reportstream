const appHome = async ({ event, client }) => {
  try {
    /* view.publish is the method that your app uses to push a view to the Home tab */
    await client.views.publish({
      /* the user that opened your app's app home */
      user_id: event.user,

      /* the view object that appears in the app home*/
      view: {
        type: "home",
        callback_id: "home_view",

        /* body of the view */
        blocks: [
          {
            type: "section",
            text: {
              type: "mrkdwn",
              text: "Welcome :tada:",
            },
          },
          {
            type: "divider",
          },
        ],
      },
    });
  } catch (error) {
    console.error(error);
  }
};

export default appHome;
