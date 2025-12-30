#!/bin/bash
# Aurigraph V11 Native Build Menu
# Interactive menu for native compilation profiles

set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'  
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Display banner
show_banner() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║               AURIGRAPH V11 NATIVE BUILD MENU               ║"
    echo "║                  Ultra-Optimized Compilation                ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Display build profiles
show_profiles() {
    echo -e "${WHITE}Available Native Build Profiles:${NC}"
    echo ""
    
    echo -e "${GREEN}1. Fast Profile (Development)${NC}"
    echo -e "   ${BLUE}•${NC} Build time: ~2 minutes"
    echo -e "   ${BLUE}•${NC} Startup time: <1 second"  
    echo -e "   ${BLUE}•${NC} Binary size: ~60MB"
    echo -e "   ${BLUE}•${NC} Use case: Rapid development iteration"
    echo ""
    
    echo -e "${YELLOW}2. Standard Profile (Production)${NC}"
    echo -e "   ${BLUE}•${NC} Build time: ~12 minutes"
    echo -e "   ${BLUE}•${NC} Startup time: <800ms"
    echo -e "   ${BLUE}•${NC} Binary size: ~75MB" 
    echo -e "   ${BLUE}•${NC} Use case: Balanced production deployment"
    echo ""
    
    echo -e "${PURPLE}3. Ultra Profile (Maximum Performance)${NC}"
    echo -e "   ${BLUE}•${NC} Build time: ~25 minutes"
    echo -e "   ${BLUE}•${NC} Startup time: <600ms"
    echo -e "   ${BLUE}•${NC} Binary size: ~85MB"
    echo -e "   ${BLUE}•${NC} Use case: 2M+ TPS high-performance deployment"
    echo ""
    
    echo -e "${CYAN}4. Test All Profiles${NC}"
    echo -e "   ${BLUE}•${NC} Comprehensive testing and benchmarking"
    echo -e "   ${BLUE}•${NC} Performance analysis and reporting"
    echo ""
    
    echo -e "${WHITE}Docker Options:${NC}"
    echo -e "${GREEN}5. Build Fast Docker Container${NC}"
    echo -e "${YELLOW}6. Build Standard Docker Container${NC}" 
    echo -e "${PURPLE}7. Build Ultra Docker Container${NC}"
    echo -e "${CYAN}8. Docker Compose (All Profiles)${NC}"
    echo ""
}

# Execute build choice
execute_choice() {
    local choice=$1
    
    case $choice in
        1)
            echo -e "${GREEN}Starting Fast Profile Build...${NC}"
            ./build-native-fast.sh
            ;;
        2)
            echo -e "${YELLOW}Starting Standard Profile Build...${NC}"
            ./build-native-standard.sh
            ;;
        3)
            echo -e "${PURPLE}Starting Ultra Profile Build...${NC}"
            echo -e "${YELLOW}Warning: This build takes ~25 minutes and requires 8GB+ RAM${NC}"
            read -p "Continue? (y/N): " confirm
            if [[ $confirm =~ ^[Yy]$ ]]; then
                ./build-native-ultra.sh
            else
                echo "Build cancelled."
                return
            fi
            ;;
        4)
            echo -e "${CYAN}Starting Comprehensive Testing...${NC}"
            ./test-native-optimization.sh
            ;;
        5)
            echo -e "${GREEN}Building Fast Docker Container...${NC}"
            docker build -f src/main/docker/Dockerfile.native-fast -t aurigraph/v11:fast .
            echo -e "${GREEN}Run with: docker run -p 9003:9003 -p 9004:9004 aurigraph/v11:fast${NC}"
            ;;
        6)
            echo -e "${YELLOW}Building Standard Docker Container...${NC}"  
            docker build -f src/main/docker/Dockerfile.native -t aurigraph/v11:standard .
            echo -e "${YELLOW}Run with: docker run -p 9003:9003 -p 9004:9004 aurigraph/v11:standard${NC}"
            ;;
        7)
            echo -e "${PURPLE}Building Ultra Docker Container...${NC}"
            docker build -f src/main/docker/Dockerfile.native-ultra-optimized -t aurigraph/v11:ultra .
            echo -e "${PURPLE}Run with: docker run -p 9003:9003 -p 9004:9004 aurigraph/v11:ultra${NC}"
            ;;
        8)
            echo -e "${CYAN}Starting Docker Compose (All Profiles)...${NC}"
            docker-compose -f docker-compose-native-profiles.yml up
            ;;
        0)
            echo -e "${WHITE}Available Commands:${NC}"
            echo "• ./build-native-fast.sh          - Fast development build"
            echo "• ./build-native-standard.sh      - Standard production build"
            echo "• ./build-native-ultra.sh         - Ultra performance build" 
            echo "• ./test-native-optimization.sh   - Test all profiles"
            echo "• ./mvnw package -Pnative-fast    - Maven fast build"
            echo "• ./mvnw package -Pnative         - Maven standard build"
            echo "• ./mvnw package -Pnative-ultra   - Maven ultra build"
            echo ""
            echo -e "${WHITE}Docker Commands:${NC}"
            echo "• docker build -f src/main/docker/Dockerfile.native-fast -t aurigraph/v11:fast ."
            echo "• docker build -f src/main/docker/Dockerfile.native-ultra-optimized -t aurigraph/v11:ultra ."
            echo "• docker-compose -f docker-compose-native-profiles.yml up"
            ;;
        *)
            echo -e "${RED}Invalid choice. Please try again.${NC}"
            return 1
            ;;
    esac
}

# Display system requirements
show_requirements() {
    echo -e "${WHITE}System Requirements:${NC}"
    echo -e "${BLUE}•${NC} Java 21+ (for local builds)"
    echo -e "${BLUE}•${NC} Docker (for container builds)"
    echo -e "${BLUE}•${NC} RAM: 4GB+ (Fast), 6GB+ (Standard), 8GB+ (Ultra)"
    echo -e "${BLUE}•${NC} Disk: 5GB+ free space"
    echo ""
}

# Main menu loop
main_menu() {
    while true; do
        clear
        show_banner
        show_requirements
        show_profiles
        
        echo -e "${WHITE}Choose an option:${NC}"
        echo -e "${GREEN}[1]${NC} Fast Build    ${YELLOW}[2]${NC} Standard Build    ${PURPLE}[3]${NC} Ultra Build    ${CYAN}[4]${NC} Test All"
        echo -e "${GREEN}[5]${NC} Docker Fast   ${YELLOW}[6]${NC} Docker Standard   ${PURPLE}[7]${NC} Docker Ultra    ${CYAN}[8]${NC} Compose All"
        echo -e "${WHITE}[0]${NC} Show Commands ${RED}[q]${NC} Quit"
        echo ""
        read -p "Enter your choice: " choice
        
        case $choice in
            q|Q|quit|exit)
                echo -e "${GREEN}Thank you for using Aurigraph V11 Native Build System!${NC}"
                exit 0
                ;;
            0|1|2|3|4|5|6|7|8)
                echo ""
                execute_choice $choice
                echo ""
                read -p "Press Enter to continue..." 
                ;;
            *)
                echo -e "${RED}Invalid choice. Please try again.${NC}"
                sleep 2
                ;;
        esac
    done
}

# Check if script is being run directly
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    # Check if we're in the right directory
    if [ ! -f "pom.xml" ] || [ ! -f "build-native-fast.sh" ]; then
        echo -e "${RED}Error: Please run this script from the aurigraph-v11-standalone directory${NC}"
        echo -e "${BLUE}Current directory: $(pwd)${NC}"
        echo -e "${BLUE}Expected files: pom.xml, build-native-fast.sh${NC}"
        exit 1
    fi
    
    # Run main menu
    main_menu
else
    # Script is being sourced, just show help
    show_banner
    echo -e "${WHITE}Native Build Menu Functions Loaded${NC}"
    echo "• Run 'main_menu' to start interactive menu"
    echo "• Run 'show_profiles' to see build profile information"
fi