#!/usr/bin/env bash
set -euo pipefail

FRONTEND_URL="${1:?Uso: aws-learner-lab.sh <url-de-amplify> <allocation-id-de-la-ip-elastica>}"
FRONTEND_URL="${FRONTEND_URL%/}"
ALLOCATION_ID="${2:?Falta el allocation id de la IP elástica}"
APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$APP_DIR/deploy/.env"

TOKEN="$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 300')"
meta() { curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" "http://169.254.169.254/latest/meta-data/$1"; }
INSTANCE_ID="$(meta instance-id)"
export AWS_DEFAULT_REGION="$(meta placement/region)"

aws ec2 associate-address --instance-id "$INSTANCE_ID" --allocation-id "$ALLOCATION_ID" > /dev/null
PUBLIC_IP="$(aws ec2 describe-addresses --allocation-ids "$ALLOCATION_ID" --query 'Addresses[0].PublicIp' --output text)"
API_DOMAIN="${PUBLIC_IP//./-}.sslip.io"

aws cloudformation deploy --stack-name eduquest-media --template-file "$APP_DIR/deploy/cloudformation.yml" \
  --parameter-overrides CreateInstanceRole=false --no-fail-on-empty-changeset
BUCKET="$(aws cloudformation describe-stacks --stack-name eduquest-media \
  --query "Stacks[0].Outputs[?OutputKey=='BucketName'].OutputValue" --output text)"

EC2_SG="$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].SecurityGroups[0].GroupId' --output text)"
VPC_ID="$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" --query 'Reservations[0].Instances[0].VpcId' --output text)"
DB_SG="$(aws ec2 describe-security-groups --filters Name=group-name,Values=eduquest-db Name=vpc-id,Values="$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text)"
if [ "$DB_SG" = "None" ]; then
  DB_SG="$(aws ec2 create-security-group --group-name eduquest-db --description "EduQuest RDS" --vpc-id "$VPC_ID" \
    --query GroupId --output text)"
  aws ec2 authorize-security-group-ingress --group-id "$DB_SG" --protocol tcp --port 5432 --source-group "$EC2_SG" > /dev/null
fi

if [ -f "$ENV_FILE" ]; then
  DB_PASSWORD="$(grep '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
  JWT_SECRET="$(grep '^JWT_SECRET=' "$ENV_FILE" | cut -d= -f2-)"
else
  DB_PASSWORD="$(openssl rand -hex 20)"
  JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
fi

if ! aws rds describe-db-instances --db-instance-identifier eduquest-db > /dev/null 2>&1; then
  aws rds create-db-instance --db-instance-identifier eduquest-db --engine postgres \
    --db-instance-class db.t3.micro --allocated-storage 20 --storage-type gp2 \
    --db-name eduquest --master-username eduquest --master-user-password "$DB_PASSWORD" \
    --vpc-security-group-ids "$DB_SG" --no-publicly-accessible --backup-retention-period 1 > /dev/null
fi

umask 077
cat > "$ENV_FILE" <<ENV
API_DOMAIN=$API_DOMAIN
CORS_ALLOWED_ORIGINS=$FRONTEND_URL
FRONTEND_URL=$FRONTEND_URL
DB_HOST=pendiente
DB_PORT=5432
DB_NAME=eduquest
DB_USER=eduquest
DB_PASSWORD=$DB_PASSWORD
DB_SSLMODE=require
STORAGE_PROVIDER=s3
AWS_REGION=$AWS_DEFAULT_REGION
S3_BUCKET=$BUCKET
JWT_SECRET=$JWT_SECRET
MAIL_USER=
MAIL_APP_PASSWORD=
GEMINI_API_KEY=
ADMIN_INITIAL_PASSWORD=
ENV

USER=ec2-user bash "$APP_DIR/deploy/setup-ec2.sh"

aws rds wait db-instance-available --db-instance-identifier eduquest-db
DB_HOST="$(aws rds describe-db-instances --db-instance-identifier eduquest-db \
  --query 'DBInstances[0].Endpoint.Address' --output text)"
sed -i "s|^DB_HOST=.*|DB_HOST=$DB_HOST|" "$ENV_FILE"

chown -R ec2-user:ec2-user "$APP_DIR"
cd "$APP_DIR/deploy"
docker compose -f docker-compose.prod.yml up -d --build

echo "Backend: https://$API_DOMAIN/api/v1"
