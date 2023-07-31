# Installing Gradle

For more information see https://gradle.org/install/

## Windows

See https://gradle.org/install/ for detailed instructions.

## macOS

```bash
brew update
brew install gradle
```

## Linux

If your package manager contains a gradle version >=7.0.0, feel free to install it using your package manager of choice. For manual installation, you can do the following:

```bash
mkdir -p ${HOME?}/bin/gradle-bins/
cd ${HOME?}/bin/gradle-bins/
VERSION=7.6.2
wget https://services.gradle.org/distributions/gradle-${VERSION?}-bin.zip
unzip "gradle-${VERSION?}-bin.zip"
rm "gradle-${VERSION?}-bin.zip"
ln -s "${HOME?}/bin/gradle-bins/gradle-${VERSION}/bin/gradle" "${HOME?}/bin/gradle"
```

Add ```${HOME}/bin``` to your path if it's not there already.
