const lock_target = async ({ localStorage, args, say, user }) => {
  const action = args[2];
  const branch = args[3];

  const deployTargets = process.env.GITHUB_TARGET_BRANCHES.split(",");
  if (!deployTargets.includes(branch)) {
    say(
      `\"${branch}\" is not in available target branches. Use:  <@bot name> gh-targets`
    );
    return false;
  }

  switch (action) {
    case "add":
      // Check if the branch already has a lock
      if (localStorage.getItem(branch)) {
        // If yes, say that the branch is already locked
        await say(
          `Branch ${branch} is already locked by <@${localStorage.getItem(
            branch
          )}>`
        );
      } else {
        // If no, add the lock and say that the branch is locked
        localStorage.setItem(branch, user);
        await say(`Locked branch ${branch}`);
      }
      break;
    case "remove":
      // Check if the branch has a lock
      if (localStorage.getItem(branch)) {
        // If yes, remove the lock and say that the lock is removed
        await say(
          `Removed <@${localStorage.getItem(
            branch
          )}>'s lock on branch ${branch}`
        );
        localStorage.removeItem(branch);
      } else {
        // If no, say that the branch is not locked
        await say(`Branch ${branch} is not locked`);
      }
      break;
    case "show":
      // Check if the branch has a lock
      if (localStorage.getItem(branch)) {
        // If yes, say who locked the branch
        await say(
          `Branch ${branch} locked by <@${localStorage.getItem(branch)}>`
        );
      } else {
        // If no, say that the branch is not locked
        await say(`Branch ${branch} is not locked`);
      }
      break;
    default:
      await say("Invalid lock command");
  }
};

export default lock_target;
