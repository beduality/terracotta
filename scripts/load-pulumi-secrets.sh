#!/bin/bash
set -e

# Path to the .env file (default to .env in current directory or parent)
ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ] && [ -f "../../.env" ]; then
  ENV_FILE="../../.env"
fi

if [ -f "$ENV_FILE" ]; then
  echo "Loading variables from $ENV_FILE..."
  # Load env variables (ignoring comments and empty lines)
  export $(grep -v '^#' "$ENV_FILE" | xargs)
else
  echo "Error: No .env file found at $ENV_FILE or ../../.env"
  exit 1
fi

# Secrets to import into Pulumi config
SECRETS=(
  "OSSRH_USERNAME"
  "OSSRH_PASSWORD"
  "SIGNING_KEY"
  "SIGNING_PASSWORD"
)

for var in "${SECRETS[@]}"; do
  # Convert to camelCase (e.g. OSSRH_USERNAME -> ossrhUsername)
  config_key=$(echo "$var" | tr '[:upper:]' '[:lower:]' | awk -F_ '{for(i=1;i<=NF;i++){if(i==1){printf "%s", $i}else{printf "%s", toupper(substr($i,1,1)) substr($i,2)}}}')
  
  # Get dynamic variable value
  val="${!var}"
  
  if [ -n "$val" ]; then
    # Automatically decode SIGNING_KEY if it is base64 encoded
    if [ "$var" = "SIGNING_KEY" ] && [[ ! "$val" =~ "BEGIN PGP" ]]; then
      if decoded=$(echo "$val" | base64 --decode 2>/dev/null); then
        val="$decoded"
        echo "Decoded base64 SIGNING_KEY"
      fi
    fi

    pulumi config set --secret "$config_key" -- "$val"
    echo "Successfully set secret: $config_key"
  else
    echo "Warning: $var is not set in $ENV_FILE (skipping)"
  fi
done
