# Rapid WSL

![](https://badgen.net/badge/icon/gitguardian/green?icon=gitguardian&label)
![](https://badgen.net/badge/icon/windows?icon=windows&label)
[![Open in Visual Studio Code](https://img.shields.io/badge/open%20in-vscode.dev-blue)](https://open.vscode.dev/JosiahSiegel/rapid-wsl)

>⚠️Disable Docker Desktop resource saver due to instability

## Synopsis

Easily manage your WSL environment with a handful of generic commands.
Applications are seperated into [modules](modules/).
Remote repository is cloned into distro if specified (`-r`).

## Quick start

```sh
./rwsl init -d "Ubuntu-18.04" -m "generic" # -d:Distro, -m:Module, -u:User, -r:Repo
```
or
```sh
./rwsl init $(cat defaults/generic.sh)
```

## Security

Place `.pgp` & `.gitconfig` files into the `etc/` directory (*ignored by git*).

Example `.gitconfig`:

```sh
[core]
   editor = "vim"
[user]
	name = First Last
	email = my@email.com
	signingKey = xxx

[commit]
	gpgSign = True
```

How to import pgp files from keybase.io:

```sh
keybase pgp export -q xxx > etc/public.pgp
keybase pgp export -q xxx --secret > etc/private.pgp
```

For GitHub authorization, add GitHub personal access token file `git.token` to `etc/`.

## Commands

1. `init` (Re)initialize env.
2. `status` Status of env.
3. `backup` Backup env to `.tar` file.
4. `down` Shutdown env.
5. `up` Bring env up after shutdown.
6. `destroy` Destroy env.
7. `restore` Restore env from `.tar` file.
8. `test` Run module specific env tests.
9. `fix` Run module specific env fixes.

The following scripts can be run in various order, but some run orders are not compatible (e.g. *destroy then test*):

```sh
d="Ubuntu"; # Distro name
m="generic"; # Module name

./rwsl init -d $d -m $m;
./rwsl status -d $d -m $m;
./rwsl backup -d $d -m $m;
./rwsl down -d $d -m $m;
./rwsl up -d $d -m $m;
./rwsl destroy -d $d -m $m;
./rwsl restore -d $d -m $m;
./rwsl test -d $d -m $m;

```
---

**Mandatory:**
 * `-d`, `--distro`  VAL  Distro name (*reference: wsl --list --online*)
 * `-m`, `--module`  VAL  Application name

**Optional:**
 * `-u`, `--user`    VAL  username
 * `-r`, `--repo`    VAL  Repository url
 * `-h`, `--help`         Prints this help

**Examples:** 
 * `./rwsl [COMMAND] -d VAL -m VAL -u VAL`
 * `./rwsl init -d 'Ubuntu-18.04' -m 'generic' -u 'user3' -r 'https://github.com/JosiahSiegel/rapid-wsl.git'`

## Tips

Quick re-use arguments:
  1. Create `defaults/my_args.sh` file
     * `-d Ubuntu-20.04 -m generic -u user3`
  2. Run script using default file
     * `./rwsl init $(cat defaults/my_args.sh)`

## Notes

Currently, only tested using Git Bash via Windows Terminal.
