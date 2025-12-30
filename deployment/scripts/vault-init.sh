#!/bin/bash
set -e

echo "🔐 Initializing HashiCorp Vault PKI for Aurigraph V11..."

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
    echo "Vault not ready, waiting..."
    sleep 2
done

echo "✅ Vault is ready"

# Enable PKI secrets engine at pki/
echo "📋 Enabling PKI secrets engine..."
vault secrets enable -path=pki pki || echo "PKI already enabled"

# Configure max lease TTL to 10 years
echo "⏰ Configuring PKI max lease TTL..."
vault secrets tune -max-lease-ttl=87600h pki

# Enable intermediate PKI
echo "📋 Enabling intermediate PKI..."
vault secrets enable -path=pki_int pki || echo "Intermediate PKI already enabled"
vault secrets tune -max-lease-ttl=43800h pki_int

# Generate root CA
echo "🏛️ Generating root CA..."
vault write -field=certificate pki/root/generate/internal \
     common_name="Aurigraph DLT Root CA" \
     organization="Aurigraph DLT Corp" \
     country="US" \
     ttl=87600h \
     key_type=rsa \
     key_bits=4096 > root_ca.crt

echo "✅ Root CA generated"

# Configure PKI URLs
echo "🌐 Configuring PKI URLs..."
vault write pki/config/urls \
     issuing_certificates="$VAULT_ADDR/v1/pki/ca" \
     crl_distribution_points="$VAULT_ADDR/v1/pki/crl" \
     ocsp_servers="$VAULT_ADDR/v1/pki/ocsp"

# Generate intermediate CSR
echo "📄 Generating intermediate CSR..."
vault write -field=csr pki_int/intermediate/generate/internal \
     common_name="Aurigraph DLT Intermediate CA" \
     organization="Aurigraph DLT Corp" \
     country="US" \
     key_type=rsa \
     key_bits=4096 > pki_intermediate.csr

# Sign intermediate CSR with root CA
echo "✍️ Signing intermediate CSR..."
vault write -field=certificate pki/root/sign-intermediate \
     csr=@pki_intermediate.csr \
     format=pem_bundle \
     ttl=43800h > intermediate.cert.pem

# Set the intermediate certificate
echo "📜 Setting intermediate certificate..."
vault write pki_int/intermediate/set-signed \
     certificate=@intermediate.cert.pem

# Configure intermediate PKI URLs
echo "🔗 Configuring intermediate PKI URLs..."
vault write pki_int/config/urls \
     issuing_certificates="$VAULT_ADDR/v1/pki_int/ca" \
     crl_distribution_points="$VAULT_ADDR/v1/pki_int/crl" \
     ocsp_servers="$VAULT_ADDR/v1/pki_int/ocsp"

# Create roles for different certificate types
echo "👥 Creating certificate roles..."

# Node certificate role
vault write pki_int/roles/node \
     allowed_domains="aurigraph.io,aurigraph.local" \
     allow_subdomains=true \
     allow_glob_domains=true \
     allow_any_name=false \
     enforce_hostnames=false \
     allow_ip_sans=true \
     server_flag=true \
     client_flag=true \
     key_type=rsa \
     key_bits=4096 \
     max_ttl=2160h \
     ttl=2160h

echo "✅ Node role created"

# Validator certificate role
vault write pki_int/roles/validator \
     allowed_domains="aurigraph.io,aurigraph.local" \
     allow_subdomains=true \
     allow_glob_domains=true \
     allow_any_name=false \
     enforce_hostnames=false \
     allow_ip_sans=true \
     server_flag=true \
     client_flag=true \
     key_type=rsa \
     key_bits=4096 \
     max_ttl=4320h \
     ttl=4320h

echo "✅ Validator role created"

# Client certificate role
vault write pki_int/roles/client \
     allowed_domains="aurigraph.io,aurigraph.local" \
     allow_subdomains=true \
     allow_glob_domains=true \
     allow_any_name=false \
     enforce_hostnames=false \
     allow_ip_sans=true \
     server_flag=false \
     client_flag=true \
     key_type=rsa \
     key_bits=4096 \
     max_ttl=8760h \
     ttl=8760h

echo "✅ Client role created"

# Healthcare provider role  
vault write pki_int/roles/healthcare \
     allowed_domains="health.aurigraph.io,aurigraph.io" \
     allow_subdomains=true \
     allow_glob_domains=true \
     allow_any_name=false \
     enforce_hostnames=false \
     allow_ip_sans=true \
     server_flag=true \
     client_flag=true \
     key_type=rsa \
     key_bits=4096 \
     max_ttl=8760h \
     ttl=8760h \
     ou="Healthcare Providers"

echo "✅ Healthcare role created"

# Bridge operator role
vault write pki_int/roles/bridge \
     allowed_domains="bridge.aurigraph.io,aurigraph.io" \
     allow_subdomains=true \
     allow_glob_domains=true \
     allow_any_name=false \
     enforce_hostnames=false \
     allow_ip_sans=true \
     server_flag=true \
     client_flag=true \
     key_type=rsa \
     key_bits=4096 \
     max_ttl=2160h \
     ttl=2160h \
     ou="Bridge Operators"

echo "✅ Bridge role created"

# Enable AppRole auth method for service authentication
echo "🔐 Enabling AppRole authentication..."
vault auth enable approle || echo "AppRole already enabled"

# Create policy for Aurigraph services
echo "📋 Creating service policy..."
vault policy write aurigraph-service - <<EOF
# PKI operations
path "pki_int/issue/*" {
  capabilities = ["create", "update"]
}

path "pki_int/revoke" {
  capabilities = ["create", "update"]
}

path "pki_int/crl" {
  capabilities = ["read"]
}

path "pki/ca/pem" {
  capabilities = ["read"]
}

# Certificate lookup
path "pki_int/cert/*" {
  capabilities = ["read"]
}

# Health checks
path "sys/health" {
  capabilities = ["read"]
}
EOF

# Create AppRole for Aurigraph services
echo "🎭 Creating AppRole for services..."
vault write auth/approle/role/aurigraph-v11 \
     token_policies="aurigraph-service" \
     token_ttl=1h \
     token_max_ttl=4h \
     bind_secret_id=true

# Get role-id and secret-id
ROLE_ID=$(vault read -field=role_id auth/approle/role/aurigraph-v11/role-id)
SECRET_ID=$(vault write -field=secret_id auth/approle/role/aurigraph-v11/secret-id)

echo "🔑 AppRole credentials:"
echo "ROLE_ID: $ROLE_ID"
echo "SECRET_ID: $SECRET_ID"

# Save credentials to file
cat > /vault/userconfig/aurigraph-credentials.txt <<EOF
# Aurigraph V11 Vault Credentials
# Generated: $(date)

VAULT_ADDR=$VAULT_ADDR
ROLE_ID=$ROLE_ID
SECRET_ID=$SECRET_ID

# Usage:
# export VAULT_ADDR=$VAULT_ADDR
# vault write auth/approle/login role_id=$ROLE_ID secret_id=$SECRET_ID
EOF

# Issue test certificate
echo "🧪 Issuing test certificate..."
vault write pki_int/issue/node \
     common_name="test-node.aurigraph.local" \
     ttl=24h > test_certificate.json

echo "✅ Test certificate issued successfully"

# Display PKI status
echo "📊 PKI Status:"
echo "Root CA:"
vault read -field=certificate pki/ca/pem | openssl x509 -text -noout | grep -E "(Subject:|Not After)"

echo ""
echo "Intermediate CA:"
vault read -field=certificate pki_int/ca/pem | openssl x509 -text -noout | grep -E "(Subject:|Not After)"

echo ""
echo "🎉 Vault PKI initialization complete!"
echo ""
echo "Next steps:"
echo "1. Set VAULT_ROLE_ID and VAULT_SECRET_ID environment variables in your application"
echo "2. Configure ca.vault.url=http://localhost:8200 in application.properties"
echo "3. Start Aurigraph V11 application"
echo ""
echo "Vault UI: http://localhost:8200/ui (token: dev-root-token-aurigraph)"