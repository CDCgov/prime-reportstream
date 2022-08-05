# Admin Functions

## Troubleshooting

1. Make sure you are not running as root:
    * `sudo -u <user> bash`
2. Navigate to function app directory:
    * `/operations/app/functions/admin`
3. Install python env:
    * `sudo apt-get install python3.8-venv`
4. If `.venv` is missing:
    * `python3 -m venv .venv`
5. Activate `.env`:
    * `source .venv/bin/activate`
6. If `local.settings.json` is missing:
    * `func init`
7. Run function app:
    * `func host start -p 8080`
8. Make sure interpreter is selected for the relevant function app:
    * `Ctrl+Shift+p` > Python: select interpreter > select function app (workspace) > "Enter interpreter path..."
9. Upgrade virtual env pip: 
    * `.venv/bin/python3 -m pip install --upgrade pip`
10. Install libraries locally: 
    * `.venv/bin/python3 -m pip install azure-functions` or `.venv/bin/python3 -m pip install -r requirements.txt`
