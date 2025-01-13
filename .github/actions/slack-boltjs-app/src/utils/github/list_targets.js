const list_targets = async ({ say }) => {
  const deployTargets = process.env.GITHUB_TARGET_BRANCHES.split(",");
  if (deployTargets.length === 0) {
    return say(
      "No targets branches defined. Set GITHUB_TARGET_BRANCHES first."
    );
  } else {
    var targets = "- " + deployTargets.join("\n- ");
    return say(targets);
  }
};

export default list_targets;
