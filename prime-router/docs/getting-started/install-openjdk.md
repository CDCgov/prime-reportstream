# Installing OpenJDK

Note that we currently target OpenJDK 11 through 15, examples use OpenJDK 11.

## Windows

See https://jdk.java.net/ for detailed instructions.

## macOS

```bash
brew update
# Set to 15 if you so choose
VERSION=11
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
brew info openjdk
# Example output from running the above command, please run yourself
# just in case brew updates the install instructions
==> Caveats
For the system Java wrappers to find this JDK, symlink it with
  sudo ln -sfn /usr/local/opt/openjdk/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk.jdk

openjdk is keg-only, which means it was not symlinked into /usr/local,
because macOS provides similar software and installing this software in
parallel can cause all kinds of trouble.

If you need to have openjdk first in your PATH, run:
  echo 'export PATH="/usr/local/opt/openjdk/bin:$PATH"' >> ~/.zshrc

For compilers to find openjdk you may need to set:
  export CPPFLAGS="-I/usr/local/opt/openjdk/include"
```

I had to follow all three commands to get `./cleanslate.sh` to run cleanly.

## Linux

### Debian-based
```bash
sudo apt-get update
# Set to 15 if you so choose
VERSION=11
sudo apt-get --yes install openjdk-${VERSION?}-jdk
```