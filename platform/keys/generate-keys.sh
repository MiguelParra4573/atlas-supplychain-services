#!/usr/bin/env sh
# Genera el par RSA 2048 que auth-server usa para firmar JWT (RS256). Solo para desarrollo local:
# los .pem están en .gitignore y nunca se versionan.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$DIR/private_key.pem"
openssl pkey -in "$DIR/private_key.pem" -pubout -out "$DIR/public_key.pem"
echo "Claves generadas en $DIR"
