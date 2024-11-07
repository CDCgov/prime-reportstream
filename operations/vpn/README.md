# VPN Administration

## File structure
  * `CDC Teams > ReportStream > DevOps - Private > Files`
    ```
    📦vpn
    ┣ 📂gen
    ┃ ┣ 📜caCert.pem
    ┃ ┣ 📜caKey.pem
    ┃ ┣ 📜prime-data-hub-demo1.ovpn
    ┃ ┣ 📜prime-data-hub-demo2.ovpn
    ┃ ┣ 📜prime-data-hub-demo3.ovpn
    ┃ ┣ 📜prime-data-hub-prod.ovpn
    ┃ ┗ 📜prime-data-hub-staging.ovpn
    ┣ 📂profiles
    ┣ 📜README.md
    ┣ 📜createKey.sh
    ┗ 📜revokeKey.sh
    ```

## Create user access

>Requires CDC OneDrive installed on local computer.

1. Open CDC Teams and navigate to `ReportStream > DevOps - Private > Files`
2. For directory `VPN`, select "Add shortcut to OneDrive"
3. In a Bash shell, navigate to your `OneDrive - CDC/vpn` directory mount
   * WSL example: `/mnt/c/Users/<user>/OneDrive\ -\ CDC/vpn/`
4. Run `./createKey.sh` and follow the prompts
5. The user's VPN profiles with be created in a folder with their name under `profiles/`
6. Send directory with user's name via CDC teams

## Revoke user access
  * ```sh
    vpn_user="user1"
    env="staging prod"

    ./revokeKey.sh $vpn_user "$env"
    ```

## Backup scripts
  * [Repo](https://github.com/CDCgov/prime-reportstream/tree/main/operations/vpn)
