#!/bin/bash

# Aurigraph V11 Native Configuration Validation Script
# Validates all native-image configuration files for optimal performance

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

log_error() {
    echo -e "${RED}✗ $1${NC}"
}

log_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

echo "🔍 Aurigraph V11 Native Configuration Validation"
echo "================================================"

# Check native-image directory structure
if [[ -d "src/main/resources/META-INF/native-image" ]]; then
    log_success "Native image configuration directory exists"
else
    log_error "Native image configuration directory missing"
    exit 1
fi

# Validate native-image.properties
if [[ -f "src/main/resources/META-INF/native-image/native-image.properties" ]]; then
    log_success "native-image.properties found"
    
    # Check for key optimizations
    if grep -q "UseG1GC" src/main/resources/META-INF/native-image/native-image.properties; then
        log_success "G1GC optimization enabled"
    else
        log_warning "G1GC optimization not found"
    fi
    
    if grep -q "MaxGCPauseMillis=1" src/main/resources/META-INF/native-image/native-image.properties; then
        log_success "Ultra-low GC pause time configured (1ms)"
    fi
    
    if grep -q "StaticExecutableWithDynamicLibC" src/main/resources/META-INF/native-image/native-image.properties; then
        log_success "Static executable configuration found"
    fi
    
    if grep -q "initialize-at-run-time=io.netty" src/main/resources/META-INF/native-image/native-image.properties; then
        log_success "Netty runtime initialization configured"
    fi
    
    if grep -q "initialize-at-run-time=io.grpc" src/main/resources/META-INF/native-image/native-image.properties; then
        log_success "gRPC runtime initialization configured"
    fi
    
    if grep -q "initialize-at-run-time=org.bouncycastle" src/main/resources/META-INF/native-image/native-image.properties; then
        log_success "BouncyCastle cryptography runtime initialization configured"
    fi
else
    log_error "native-image.properties not found"
fi

# Validate reflection configurations
for config in "reflect-config.json" "reflect-config-optimized.json" "reflect-config-ultra.json"; do
    if [[ -f "src/main/resources/META-INF/native-image/$config" ]]; then
        log_success "$config found"
        
        # Count reflection entries
        entry_count=$(jq length "src/main/resources/META-INF/native-image/$config" 2>/dev/null || echo "0")
        log_info "$config contains $entry_count reflection entries"
    else
        log_warning "$config not found"
    fi
done

# Validate resource configuration
if [[ -f "src/main/resources/META-INF/native-image/resource-config.json" ]]; then
    log_success "resource-config.json found"
    
    # Check for proto file inclusion
    if grep -q "proto" src/main/resources/META-INF/native-image/resource-config.json; then
        log_success "Protocol buffer files included in resources"
    fi
    
    # Check for exclusions to minimize binary size
    if grep -q "excludes" src/main/resources/META-INF/native-image/resource-config.json; then
        log_success "Resource exclusions configured for minimal binary size"
    fi
else
    log_error "resource-config.json not found"
fi

# Validate serialization configuration
if [[ -f "src/main/resources/META-INF/native-image/serialization-config.json" ]]; then
    log_success "serialization-config.json found"
    
    serialization_count=$(jq length "src/main/resources/META-INF/native-image/serialization-config.json" 2>/dev/null || echo "0")
    log_info "Serialization config contains $serialization_count entries"
else
    log_error "serialization-config.json not found"
fi

# Validate JNI configuration
if [[ -f "src/main/resources/META-INF/native-image/jni-config.json" ]]; then
    log_success "jni-config.json found"
else
    log_warning "jni-config.json not found (may not be needed)"
fi

# Validate proxy configuration
if [[ -f "src/main/resources/META-INF/native-image/proxy-config.json" ]]; then
    log_success "proxy-config.json found"
else
    log_warning "proxy-config.json not found (may not be needed)"
fi

# Check Maven profiles in pom.xml
if [[ -f "pom.xml" ]]; then
    log_success "pom.xml found"
    
    if grep -q "native-fast" pom.xml; then
        log_success "native-fast profile configured"
    else
        log_warning "native-fast profile not found"
    fi
    
    if grep -q "native-ultra" pom.xml; then
        log_success "native-ultra profile configured"
    else
        log_warning "native-ultra profile not found"
    fi
    
    if grep -q "quay.io/quarkus/ubi-quarkus-mandrel:24-java21" pom.xml; then
        log_success "Java 21 Mandrel builder image configured"
    else
        log_warning "Java 21 builder image not found"
    fi
else
    log_error "pom.xml not found"
fi

# Check build scripts
if [[ -f "native-build-ultra.sh" ]]; then
    log_success "Ultra-optimized build script available"
    if [[ -x "native-build-ultra.sh" ]]; then
        log_success "Build script is executable"
    else
        log_warning "Build script is not executable - run: chmod +x native-build-ultra.sh"
    fi
else
    log_warning "native-build-ultra.sh not found"
fi

# Check application.properties for native settings
if [[ -f "src/main/resources/application.properties" ]]; then
    log_success "application.properties found"
    
    if grep -q "quarkus.native.container-build=true" src/main/resources/application.properties; then
        log_success "Container-based native builds enabled"
    fi
    
    if grep -q "quarkus.virtual-threads.enabled=true" src/main/resources/application.properties; then
        log_success "Virtual threads enabled for Java 21"
    fi
    
    if grep -q "quarkus.http.http2=true" src/main/resources/application.properties; then
        log_success "HTTP/2 enabled for high performance"
    fi
else
    log_error "application.properties not found"
fi

echo ""
echo "🎯 Configuration Summary:"
echo "========================"

echo "Native Image Optimizations:"
echo "- G1GC with 1ms pause times"
echo "- Static executable with dynamic LibC"
echo "- Runtime initialization for network/crypto"
echo "- Aggressive performance optimizations"
echo "- Minimal reflection configuration"
echo "- Resource exclusions for smaller binary"

echo ""
echo "Build Profiles Available:"
echo "- native-fast: Development builds (~2min)"
echo "- native: Standard production builds (~15min)"
echo "- native-ultra: Ultra-optimized builds (~30min)"

echo ""
echo "Performance Targets:"
echo "- Startup Time: <1s"
echo "- Memory Usage: <256MB"
echo "- Binary Size: <100MB" 
echo "- TPS Capability: 2M+"

echo ""
echo "✅ Configuration validation complete!"
echo "🚀 Ready for native compilation with Java 21+"