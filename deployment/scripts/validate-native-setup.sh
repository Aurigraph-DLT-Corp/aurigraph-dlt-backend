#!/bin/bash
# Aurigraph V11 Native Compilation Setup Validator
# 
# This script validates that the complete GraalVM native compilation setup
# is correctly configured and ready for production deployment.

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALIDATION_LOG="$SCRIPT_DIR/native-setup-validation.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$VALIDATION_LOG"
}

log_pass() {
    echo -e "${GREEN}[PASS]${NC} $1" | tee -a "$VALIDATION_LOG"
    ((PASSED++))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1" | tee -a "$VALIDATION_LOG"
    ((FAILED++))
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$VALIDATION_LOG"
    ((WARNINGS++))
}

log_header() {
    echo -e "\n${BOLD}=== $1 ===${NC}" | tee -a "$VALIDATION_LOG"
}

# Initialize validation log
init_validation() {
    echo "=== Aurigraph V11 Native Compilation Setup Validation ===" > "$VALIDATION_LOG"
    echo "Timestamp: $(date)" >> "$VALIDATION_LOG"
    echo "Working Directory: $SCRIPT_DIR" >> "$VALIDATION_LOG"
    echo "" >> "$VALIDATION_LOG"
    
    log_header "Validating Aurigraph V11 Native Compilation Setup"
}

# Check project structure
validate_project_structure() {
    log_header "Project Structure Validation"
    
    # Core Maven files
    if [ -f "pom.xml" ]; then
        log_pass "pom.xml exists"
        
        # Check for native profiles
        if grep -q "native-ultra" pom.xml; then
            log_pass "native-ultra profile configured"
        else
            log_fail "native-ultra profile missing in pom.xml"
        fi
        
        if grep -q "native-fast" pom.xml; then
            log_pass "native-fast profile configured"
        else
            log_fail "native-fast profile missing in pom.xml"
        fi
    else
        log_fail "pom.xml not found"
    fi
    
    # Maven wrapper
    if [ -f "mvnw" ] && [ -x "mvnw" ]; then
        log_pass "Maven wrapper exists and is executable"
    else
        log_fail "Maven wrapper missing or not executable"
    fi
    
    # Java source structure
    if [ -d "src/main/java" ]; then
        log_pass "Java source directory exists"
        
        local java_files=$(find src/main/java -name "*.java" | wc -l)
        log_info "Found $java_files Java source files"
        
        if [ "$java_files" -gt 0 ]; then
            log_pass "Java source files present"
        else
            log_fail "No Java source files found"
        fi
    else
        log_fail "Java source directory missing"
    fi
    
    # Resources directory
    if [ -d "src/main/resources" ]; then
        log_pass "Resources directory exists"
    else
        log_fail "Resources directory missing"
    fi
}

# Validate native image configuration
validate_native_config() {
    log_header "Native Image Configuration Validation"
    
    local config_dir="src/main/resources/META-INF/native-image"
    
    if [ -d "$config_dir" ]; then
        log_pass "Native image configuration directory exists"
        
        # Check each configuration file
        local config_files=("reflect-config.json" "serialization-config.json" "jni-config.json" "proxy-config.json" "resource-config.json" "native-image.properties")
        
        for config_file in "${config_files[@]}"; do
            if [ -f "$config_dir/$config_file" ]; then
                log_pass "$config_file exists"
                
                # Validate JSON files
                if [[ "$config_file" == *.json ]]; then
                    if command -v jq >/dev/null 2>&1; then
                        if jq empty "$config_dir/$config_file" >/dev/null 2>&1; then
                            log_pass "$config_file has valid JSON syntax"
                        else
                            log_fail "$config_file has invalid JSON syntax"
                        fi
                    else
                        log_warning "jq not available, skipping JSON validation for $config_file"
                    fi
                fi
            else
                log_fail "$config_file missing"
            fi
        done
        
        # Check reflection config content
        if [ -f "$config_dir/reflect-config.json" ]; then
            local reflect_classes=$(jq 'length' "$config_dir/reflect-config.json" 2>/dev/null || echo "0")
            log_info "Reflection configuration includes $reflect_classes classes"
            
            if [ "$reflect_classes" -gt 10 ]; then
                log_pass "Comprehensive reflection configuration"
            else
                log_warning "Limited reflection configuration may cause runtime issues"
            fi
        fi
        
    else
        log_fail "Native image configuration directory missing"
    fi
    
    # Check application properties
    if [ -f "src/main/resources/application.properties" ]; then
        log_pass "application.properties exists"
        
        # Check for native-specific properties
        if grep -q "quarkus.native" src/main/resources/application.properties; then
            log_pass "Native compilation properties configured"
        else
            log_warning "No native-specific properties found"
        fi
        
        # Check for performance tuning
        if grep -q "quarkus.virtual-threads.enabled=true" src/main/resources/application.properties; then
            log_pass "Virtual threads enabled for performance"
        else
            log_warning "Virtual threads not explicitly enabled"
        fi
        
    else
        log_fail "application.properties missing"
    fi
}

# Validate Docker configuration
validate_docker_config() {
    log_header "Docker Configuration Validation"
    
    local docker_dir="src/main/docker"
    
    if [ -d "$docker_dir" ]; then
        log_pass "Docker directory exists"
        
        # Check Dockerfiles
        local dockerfiles=("Dockerfile.native" "Dockerfile.native-micro" "Dockerfile.native-optimized")
        
        for dockerfile in "${dockerfiles[@]}"; do
            if [ -f "$docker_dir/$dockerfile" ]; then
                log_pass "$dockerfile exists"
                
                # Check for optimization flags
                if grep -q "HEALTHCHECK" "$docker_dir/$dockerfile"; then
                    log_pass "$dockerfile includes health checks"
                else
                    log_warning "$dockerfile missing health checks"
                fi
                
                if grep -q "USER.*1001\|USER.*nonroot" "$docker_dir/$dockerfile"; then
                    log_pass "$dockerfile uses non-root user"
                else
                    log_warning "$dockerfile may be running as root"
                fi
                
            else
                log_fail "$dockerfile missing"
            fi
        done
        
    else
        log_fail "Docker directory missing"
    fi
}

# Validate build scripts
validate_build_scripts() {
    log_header "Build Scripts Validation"
    
    # Check main build scripts
    local scripts=("build-native.sh" "quick-native-build.sh" "performance-benchmark.sh")
    
    for script in "${scripts[@]}"; do
        if [ -f "$script" ]; then
            log_pass "$script exists"
            
            if [ -x "$script" ]; then
                log_pass "$script is executable"
            else
                log_warning "$script is not executable (run: chmod +x $script)"
            fi
            
            # Check script content
            if grep -q "set -euo pipefail" "$script"; then
                log_pass "$script has proper error handling"
            else
                log_warning "$script may not have robust error handling"
            fi
            
        else
            log_fail "$script missing"
        fi
    done
}

# Validate Kubernetes configuration
validate_kubernetes_config() {
    log_header "Kubernetes Configuration Validation"
    
    local k8s_dir="k8s"
    
    if [ -d "$k8s_dir" ]; then
        log_pass "Kubernetes directory exists"
        
        # Check manifests
        local manifests=("deployment.yaml" "configmap.yaml" "hpa-vpa.yaml" "storage.yaml")
        
        for manifest in "${manifests[@]}"; do
            if [ -f "$k8s_dir/$manifest" ]; then
                log_pass "$manifest exists"
                
                # Basic YAML validation
                if command -v yq >/dev/null 2>&1; then
                    if yq eval '.' "$k8s_dir/$manifest" >/dev/null 2>&1; then
                        log_pass "$manifest has valid YAML syntax"
                    else
                        log_fail "$manifest has invalid YAML syntax"
                    fi
                elif command -v python3 >/dev/null 2>&1; then
                    if python3 -c "import yaml; yaml.safe_load(open('$k8s_dir/$manifest'))" 2>/dev/null; then
                        log_pass "$manifest has valid YAML syntax"
                    else
                        log_fail "$manifest has invalid YAML syntax"
                    fi
                else
                    log_warning "No YAML validator available, skipping syntax check for $manifest"
                fi
            else
                log_fail "$manifest missing"
            fi
        done
        
        # Check deployment script
        if [ -f "deploy-k8s.sh" ]; then
            log_pass "deploy-k8s.sh exists"
            
            if [ -x "deploy-k8s.sh" ]; then
                log_pass "deploy-k8s.sh is executable"
            else
                log_warning "deploy-k8s.sh is not executable"
            fi
        else
            log_fail "deploy-k8s.sh missing"
        fi
        
    else
        log_fail "Kubernetes directory missing"
    fi
}

# Check system requirements
validate_system_requirements() {
    log_header "System Requirements Validation"
    
    # Check available memory
    if command -v free >/dev/null 2>&1; then
        local available_memory=$(free -m | awk 'NR==2{print $7}')
        log_info "Available memory: ${available_memory}MB"
        
        if [ "$available_memory" -gt 8192 ]; then
            log_pass "Sufficient memory for native-ultra builds (>8GB)"
        elif [ "$available_memory" -gt 4096 ]; then
            log_pass "Sufficient memory for standard native builds (>4GB)"
            log_warning "Consider more memory for native-ultra builds"
        else
            log_warning "Limited memory may cause native build failures (<4GB)"
        fi
    else
        log_warning "Cannot check available memory on this system"
    fi
    
    # Check disk space
    local available_space=$(df . | tail -1 | awk '{print $4}')
    local available_space_gb=$((available_space / 1024 / 1024))
    
    log_info "Available disk space: ${available_space_gb}GB"
    
    if [ "$available_space_gb" -gt 10 ]; then
        log_pass "Sufficient disk space for builds (>10GB)"
    else
        log_warning "Limited disk space may cause build issues (<10GB)"
    fi
    
    # Check CPU cores
    if command -v nproc >/dev/null 2>&1; then
        local cpu_cores=$(nproc)
        log_info "CPU cores: $cpu_cores"
        
        if [ "$cpu_cores" -gt 4 ]; then
            log_pass "Sufficient CPU cores for parallel builds (>4)"
        else
            log_warning "Build performance may be limited with <4 CPU cores"
        fi
    else
        log_warning "Cannot check CPU cores on this system"
    fi
}

# Check tools availability
validate_tools() {
    log_header "Required Tools Validation"
    
    # Essential tools for native compilation
    local tools=("curl" "bc")
    local optional_tools=("docker" "kubectl" "jq" "yq")
    
    for tool in "${tools[@]}"; do
        if command -v "$tool" >/dev/null 2>&1; then
            log_pass "$tool is available"
        else
            log_fail "$tool is required but not available"
        fi
    done
    
    for tool in "${optional_tools[@]}"; do
        if command -v "$tool" >/dev/null 2>&1; then
            local version=$($tool --version 2>/dev/null | head -1 || echo "unknown")
            log_pass "$tool is available ($version)"
        else
            log_warning "$tool is not available (optional but recommended)"
        fi
    done
    
    # Check Java (most important)
    if command -v java >/dev/null 2>&1; then
        local java_version=$(java -version 2>&1 | head -1)
        log_pass "Java is available: $java_version"
        
        # Check for Java 21+
        if java -version 2>&1 | grep -q "21\|22\|23"; then
            log_pass "Java version is 21+ (optimal for native compilation)"
        else
            log_warning "Java version is <21, consider upgrading for best performance"
        fi
        
        # Check for GraalVM
        if java -version 2>&1 | grep -q "GraalVM"; then
            log_pass "GraalVM detected (optimal for native compilation)"
        else
            log_info "OpenJDK detected (will use container build for native compilation)"
        fi
        
    else
        log_fail "Java is not available (required for native compilation)"
    fi
}

# Validate configuration consistency
validate_configuration_consistency() {
    log_header "Configuration Consistency Validation"
    
    # Check port consistency between application.properties and Dockerfiles
    if [ -f "src/main/resources/application.properties" ]; then
        local app_http_port=$(grep "quarkus.http.port" src/main/resources/application.properties | cut -d'=' -f2 | tr -d ' ')
        local app_grpc_port=$(grep "quarkus.grpc.server.port" src/main/resources/application.properties | cut -d'=' -f2 | tr -d ' ')
        
        log_info "Application ports: HTTP=$app_http_port, gRPC=$app_grpc_port"
        
        # Check Dockerfile consistency
        if [ -f "src/main/docker/Dockerfile.native-micro" ]; then
            if grep -q "EXPOSE.*$app_http_port" src/main/docker/Dockerfile.native-micro && \
               grep -q "EXPOSE.*$app_grpc_port" src/main/docker/Dockerfile.native-micro; then
                log_pass "Docker ports match application configuration"
            else
                log_warning "Docker ports may not match application configuration"
            fi
        fi
        
        # Check Kubernetes consistency
        if [ -f "k8s/deployment.yaml" ]; then
            if grep -q "containerPort: $app_http_port" k8s/deployment.yaml && \
               grep -q "containerPort: $app_grpc_port" k8s/deployment.yaml; then
                log_pass "Kubernetes ports match application configuration"
            else
                log_warning "Kubernetes ports may not match application configuration"
            fi
        fi
    fi
    
    # Check performance targets consistency
    if [ -f "src/main/resources/application.properties" ] && [ -f "performance-benchmark.sh" ]; then
        local config_tps=$(grep "consensus.target.tps" src/main/resources/application.properties | cut -d'=' -f2 | tr -d ' ')
        
        if grep -q "TARGET_TPS.*2000000" performance-benchmark.sh; then
            log_pass "Performance benchmark targets align with production goals"
        else
            log_warning "Performance benchmark targets may not align with configuration"
        fi
    fi
}

# Generate validation report
generate_validation_report() {
    log_header "Validation Summary Report"
    
    local total_checks=$((PASSED + FAILED + WARNINGS))
    local pass_rate=0
    
    if [ "$total_checks" -gt 0 ]; then
        pass_rate=$(echo "scale=1; $PASSED * 100 / $total_checks" | bc)
    fi
    
    echo -e "\n${BOLD}VALIDATION RESULTS:${NC}" | tee -a "$VALIDATION_LOG"
    echo -e "${GREEN}Passed: $PASSED${NC}" | tee -a "$VALIDATION_LOG"
    echo -e "${RED}Failed: $FAILED${NC}" | tee -a "$VALIDATION_LOG"
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}" | tee -a "$VALIDATION_LOG"
    echo -e "${BOLD}Total Checks: $total_checks${NC}" | tee -a "$VALIDATION_LOG"
    echo -e "${BOLD}Pass Rate: ${pass_rate}%${NC}" | tee -a "$VALIDATION_LOG"
    
    # Overall status
    if [ "$FAILED" -eq 0 ]; then
        if [ "$WARNINGS" -eq 0 ]; then
            echo -e "\n${GREEN}${BOLD}✅ SETUP VALIDATION PASSED${NC}" | tee -a "$VALIDATION_LOG"
            echo -e "${GREEN}Your Aurigraph V11 native compilation setup is fully ready for production!${NC}" | tee -a "$VALIDATION_LOG"
        else
            echo -e "\n${YELLOW}${BOLD}⚠️  SETUP VALIDATION PASSED WITH WARNINGS${NC}" | tee -a "$VALIDATION_LOG"
            echo -e "${YELLOW}Your setup is functional but consider addressing the warnings for optimal performance.${NC}" | tee -a "$VALIDATION_LOG"
        fi
    else
        echo -e "\n${RED}${BOLD}❌ SETUP VALIDATION FAILED${NC}" | tee -a "$VALIDATION_LOG"
        echo -e "${RED}Please address the failed checks before proceeding with native compilation.${NC}" | tee -a "$VALIDATION_LOG"
    fi
    
    # Next steps
    echo -e "\n${BOLD}NEXT STEPS:${NC}" | tee -a "$VALIDATION_LOG"
    
    if [ "$FAILED" -eq 0 ]; then
        echo "1. Run quick build: ./quick-native-build.sh" | tee -a "$VALIDATION_LOG"
        echo "2. Run performance test: ./performance-benchmark.sh" | tee -a "$VALIDATION_LOG"
        echo "3. Build Docker image: docker build -f src/main/docker/Dockerfile.native-micro ." | tee -a "$VALIDATION_LOG"
        echo "4. Deploy to Kubernetes: ./deploy-k8s.sh dev deploy" | tee -a "$VALIDATION_LOG"
    else
        echo "1. Review failed checks above" | tee -a "$VALIDATION_LOG"
        echo "2. Install missing dependencies" | tee -a "$VALIDATION_LOG"
        echo "3. Fix configuration issues" | tee -a "$VALIDATION_LOG"
        echo "4. Re-run validation: ./validate-native-setup.sh" | tee -a "$VALIDATION_LOG"
    fi
    
    echo -e "\n${BOLD}RESOURCES:${NC}" | tee -a "$VALIDATION_LOG"
    echo "- Setup Guide: NATIVE-COMPILATION-GUIDE.md" | tee -a "$VALIDATION_LOG"
    echo "- Validation Log: $VALIDATION_LOG" | tee -a "$VALIDATION_LOG"
    echo "- Performance Targets: <1s startup, <100MB binary, 2M+ TPS" | tee -a "$VALIDATION_LOG"
}

# Main execution
main() {
    cd "$SCRIPT_DIR"
    
    init_validation
    validate_project_structure
    validate_native_config
    validate_docker_config
    validate_build_scripts
    validate_kubernetes_config
    validate_system_requirements
    validate_tools
    validate_configuration_consistency
    generate_validation_report
    
    # Return appropriate exit code
    if [ "$FAILED" -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# Execute main function
main "$@"