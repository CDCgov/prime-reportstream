# Installing OpenJDK

Note that we currently target OpenJDK 17.

## Windows

See https://jdk.java.net/ for detailed instructions.

## macOS

```bash
brew update
VERSION=17
brew install openjdk@${VERSION?}
```

If after running `./cleanslate.sh` you see the following error

```bash
./cleanslate.sh
# ...
INFO: Bringing up the minimum build dependencies
The operation couldn’t be completed. Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
```

You need to follow the rest of the brew install instructions:

```bash
VERSION=17
brew info openjdk@${VERSION?}
# Example output from running the above command, please run yourself
# just in case brew updates the install instructions
==> Caveats
For the system Java wrappers to find this JDK, symlink it with
  sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

openjdk@17 is keg-only, which means it was not symlinked into /opt/homebrew,
because this is an alternate version of another formula.

If you need to have openjdk@17 first in your PATH, run:
  echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc

For compilers to find openjdk@17 you may need to set:
  export CPPFLAGS="-I/opt/homebrew/opt/openjdk@17/include"
```

I had to follow all three commands to get `./cleanslate.sh` to run cleanly.

After installing openjdk, run the following command to set the `JAVA_HOME` variable for Azure Functions to use:

```bash
echo export "JAVA_HOME=\$(/usr/libexec/java_home)" >> ~/.zshrc
```

## Linux

### Debian-based

```bash
sudo apt-get update
# Set to 15 if you so choose
VERSION=17
sudo apt-get --yes install openjdk-${VERSION?}-jdk
```