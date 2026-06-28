build() {
    color = "\e[38;2;240;113;120m"
    reset = "\e[0m"
    error = "\e[38;2;255;51;0m"
    bold  = "\e[1m"

    echo -e "${color} ▄▀█ █▀█ █▀▄▀█   █▄▄ █ █ █ █   █▀▄ █▀▀ █▀█${reset}"
    echo -e "${color} █▀█ █▀▄ █ ▀ █   █▄█ █▄█ █ █▄▄ █▄▀ ██▄ █▀▄${reset}"

    arch = $(uname -m)
    pwd  = $(pwd)
    case "$arch" in
        aarch64|arm64|armv7l|armv8l|arm)
            ./gradlew -Pandroid.aapt2FromMavenOverride="${pwd}/bin/aapt2" build
        ;;
        
        *)
            echo -e "${error}error:${reset} not an ARM architecture: ${bold}${arch}${reset}"
            return 1
        ;;
    esac
}
build