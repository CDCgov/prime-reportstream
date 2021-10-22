# Signing Commits

(See also https://github.com/CDCgov/prime-reportstream/discussions/1278)

1. Generate a GPG key (you will need the GPG toolchain installed on your machine for this):

    ```bash
    $ gpg2 --full-generate-key
    Please select what kind of key you want:
    (1) RSA and RSA (default)
    (2) DSA and Elgamal
    (3) DSA (sign only)
    (4) RSA (sign only)
    (14) Existing key from card
    Your selection? 1
    RSA keys may be between 1024 and 4096 bits long.
    What keysize do you want? (3072) 4096
    Requested keysize is 4096 bits
    Please specify how long the key should be valid.
            0 = key does not expire
        <n>  = key expires in n days
        <n>w = key expires in n weeks
        <n>m = key expires in n months
        <n>y = key expires in n years
    Key is valid for? (0) 0
    Key does not expire at all
    Is this correct? (y/N) y

    GnuPG needs to construct a user ID to identify your key.

    Real name: My Real Name
    Email address: real.name@organization.tld
    Comment:
    You selected this USER-ID:
        "My Real Name <real.name@organization.tld>"

    Change (N)ame, (C)omment, (E)mail or (O)kay/(Q)uit? o
    We need to generate a lot of random bytes. It is a good idea to perform
    some other action (type on the keyboard, move the mouse, utilize the
    disks) during the prime generation; this gives the random number
    generator a better chance to gain enough entropy.
    gpg: key B3EF54F10C4CF2EF marked as ultimately trusted
    gpg: revocation certificate stored as '${HOME}/.gnupg/openpgp-revocs.d/E94CEC3DE7ADF3DC03603099B3EF54F10C4CF2EF.rev'
    public and secret key created and signed.

    pub   rsa4096 2021-06-14 [SC]
        E94CEC3DE7ADF3DC03603099B3EF54F10C4CF2EF
    uid                      My Real Name <real.name@organization.tld>
    sub   rsa4096 2021-06-14 [E]
    ```

1. To configure git to always sign, put this in your global gitconfig file (`~/.gitconfig` on Linux/Mac). Note that the values for `name`, `email` and `signingKey` value must match _your own_ identity. (here I'm just using the bogus values I generated above)!. If you editor has git integration, it should know how to deal with this configuration and prompt your for your key's password if that were to be needed.

1. See also: https://git-scm.com/docs/git-config

    ```ini
    [user]
        name = My Real Name
        email = real.name@organization.tld
        signingKey = E94CEC3DE7ADF3DC03603099B3EF54F10C4CF2EF

    [commit]
        gpgSign = True
    ```

    Alternatively, you can also manually sign by specifying `-S` on the command line:

    ```bash
    git commit -S
    ```

    Alternatively, you can put this configuration in your _local_ gitconfig file (located in `${REPO_ROOT?}/.git/config`) as well if you want to have different keys for different repos.

    If you are on a _mac_, you may have to specify the following command in your `${HOME}/.bash_profile` (or equivalent):

    ```bash
    export GPG_TTY=$(tty)
    ```

1. Export your key so you can later put it in GitHub

    ```bash
    $ gpg2 --armor --export real.name@organization.tld
    -----BEGIN PGP PUBLIC KEY BLOCK-----

    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
    PlGtpK8=
    =1TvH
    -----END PGP PUBLIC KEY BLOCK-----
    ```

1. Copy everything between `-----BEGIN PGP PUBLIC KEY BLOCK-----` and `-----END PGP PUBLIC KEY BLOCK-----` (including those markers)
1. Go to.  https://github.com/settings/keys; click 'New GPG key'; paste the content there and hit `Add GPG key`