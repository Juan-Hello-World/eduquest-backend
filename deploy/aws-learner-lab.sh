#!/usr/bin/env bash
set -euo pipefail

FRONTEND_URL="${1:-}"
FRONTEND_URL="${FRONTEND_URL%/}"
REPO_URL="${REPO_URL:-https://github.com/Juan-Hello-World/eduquest-backend.git}"
REGION="$(aws configure get region || echo us-east-1)"
STATE="$HOME/eduquest-deploy.env"
touch "$STATE"
# shellcheck disable=SC1090
source "$STATE"

save() { grep -v "^$1=" "$STATE" > "$STATE.tmp" || true; echo "$1=$2" >> "$STATE.tmp"; mv "$STATE.tmp" "$STATE"; }

if [ -z "${ALLOCATION_ID:-}" ]; then
  read -r ALLOCATION_ID PUBLIC_IP < <(aws ec2 allocate-address --domain vpc --query '[AllocationId,PublicIp]' --output text)
  save ALLOCATION_ID "$ALLOCATION_ID"; save PUBLIC_IP "$PUBLIC_IP"
fi
API_DOMAIN="${PUBLIC_IP//./-}.sslip.io"
echo "Backend: https://$API_DOMAIN"
if [ -z "$FRONTEND_URL" ]; then
  echo "VITE_API_BASE_URL para Amplify: https://$API_DOMAIN/api/v1"
  echo "Cuando tengas la URL de Amplify: bash $0 https://main.xxxx.amplifyapp.com"
  exit 0
fi

echo "Creando el bucket S3..."
aws cloudformation deploy --stack-name eduquest-media --template-file "$(dirname "$0")/cloudformation.yml" \
  --parameter-overrides FrontendOrigin="$FRONTEND_URL" CreateInstanceRole=false --no-fail-on-empty-changeset
BUCKET="$(aws cloudformation describe-stacks --stack-name eduquest-media --query "Stacks[0].Outputs[?OutputKey=='BucketName'].OutputValue" --output text)"

VPC_ID="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
MY_CIDR="$(curl -fsS https://checkip.amazonaws.com)/32"

sg() {
  aws ec2 describe-security-groups --filters Name=group-name,Values="$1" Name=vpc-id,Values="$VPC_ID" --query 'SecurityGroups[0].GroupId' --output text
}
EC2_SG="$(sg eduquest-backend)"
if [ "$EC2_SG" = "None" ]; then
  EC2_SG="$(aws ec2 create-security-group --group-name eduquest-backend --description "EduQuest backend" --vpc-id "$VPC_ID" --query GroupId --output text)"
  aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" --protocol tcp --port 80 --cidr 0.0.0.0/0 > /dev/null
  aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" --protocol tcp --port 443 --cidr 0.0.0.0/0 > /dev/null
  aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" --protocol tcp --port 22 --cidr 0.0.0.0/0 > /dev/null
fi
DB_SG="$(sg eduquest-db)"
if [ "$DB_SG" = "None" ]; then
  DB_SG="$(aws ec2 create-security-group --group-name eduquest-db --description "EduQuest RDS" --vpc-id "$VPC_ID" --query GroupId --output text)"
  aws ec2 authorize-security-group-ingress --group-id "$DB_SG" --protocol tcp --port 5432 --source-group "$EC2_SG" > /dev/null
fi

if [ -z "${DB_PASSWORD:-}" ]; then
  save DB_PASSWORD "$(openssl rand -hex 20)"
  save JWT_SECRET "$(openssl rand -base64 64 | tr -d '\n')"
  source "$STATE"
fi

if ! aws rds describe-db-instances --db-instance-identifier eduquest-db > /dev/null 2>&1; then
  echo "Creando la base de datos en RDS..."
  aws rds create-db-instance --db-instance-identifier eduquest-db --engine postgres \
    --db-instance-class db.t3.micro --allocated-storage 20 --storage-type gp2 \
    --db-name eduquest --master-username eduquest --master-user-password "$DB_PASSWORD" \
    --vpc-security-group-ids "$DB_SG" --no-publicly-accessible --backup-retention-period 1 > /dev/null
fi
echo "Esperando a que RDS esté disponible (5 a 10 minutos)..."
aws rds wait db-instance-available --db-instance-identifier eduquest-db
DB_HOST="$(aws rds describe-db-instances --db-instance-identifier eduquest-db --query 'DBInstances[0].Endpoint.Address' --output text)"

INSTANCE_ID="$(aws ec2 describe-instances --filters Name=tag:Name,Values=eduquest-backend Name=instance-state-name,Values=pending,running,stopped \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)"
if [ "$INSTANCE_ID" = "None" ]; then
  AMI_ID="$(aws ec2 describe-images --owners amazon --filters 'Name=name,Values=al2023-ami-2023*-x86_64' Name=state,Values=available \
    --query 'sort_by(Images,&CreationDate)[-1].ImageId' --output text)"
  USER_DATA="$(mktemp)"
  cat > "$USER_DATA" <<DATA
#!/bin/bash
set -e
dnf install -y git
git clone $REPO_URL /opt/eduquest
cd /opt/eduquest
USER=ec2-user bash deploy/setup-ec2.sh
cat > deploy/.env <<ENV
API_DOMAIN=$API_DOMAIN
CORS_ALLOWED_ORIGINS=$FRONTEND_URL
FRONTEND_URL=$FRONTEND_URL
DB_HOST=$DB_HOST
DB_PORT=5432
DB_NAME=eduquest
DB_USER=eduquest
DB_PASSWORD=$DB_PASSWORD
DB_SSLMODE=require
STORAGE_PROVIDER=s3
AWS_REGION=$REGION
S3_BUCKET=$BUCKET
JWT_SECRET=$JWT_SECRET
MAIL_USER=
MAIL_APP_PASSWORD=
GEMINI_API_KEY=
ADMIN_INITIAL_PASSWORD=
ENV
chmod 600 deploy/.env
chown -R ec2-user:ec2-user /opt/eduquest
cd deploy && docker compose -f docker-compose.prod.yml up -d --build
DATA
  INSTANCE_ID="$(aws ec2 run-instances --image-id "$AMI_ID" --instance-type t3.small \
    --iam-instance-profile Name=LabInstanceProfile --key-name vockey --security-group-ids "$EC2_SG" \
    --metadata-options HttpTokens=required,HttpPutResponseHopLimit=2,HttpEndpoint=enabled \
    --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=20,VolumeType=gp3}' \
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=eduquest-backend}]' \
    --user-data "file://$USER_DATA" --query 'Instances[0].InstanceId' --output text)"
  rm -f "$USER_DATA"
fi
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
aws ec2 associate-address --instance-id "$INSTANCE_ID" --allocation-id "$ALLOCATION_ID" > /dev/null

echo
echo "Listo."
echo "  EC2:     $INSTANCE_ID ($PUBLIC_IP)"
echo "  RDS:     $DB_HOST"
echo "  Bucket:  $BUCKET"
echo "  Backend: https://$API_DOMAIN/api/v1"
