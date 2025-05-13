### Specify environment & Terraform path
```bash
env=demo2
path='operations/app/terraform/vars/demo'

terraform -chdir=$path init \
-reconfigure \
-var-file=$env/env.tfvars.json \
-backend-config=$env/env.tfbackend

terraform -chdir=$path plan \
-var-file=$env/env.tfvars.json 
```
