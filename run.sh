#!/bin/sh
set -eu

cd "$(dirname "$0")"
project_dir=$(pwd -P)

usable_jdk() {
    [ -n "$1" ] && [ -x "$1/bin/java" ] && [ -x "$1/bin/javac" ] &&
        "$1/bin/javac" -version 2>&1 | grep -Eq '^javac 2[1-5]([. +]|$)'
}

if ! usable_jdk "${JAVA_HOME:-}"; then
    java_home=''
    case $(uname -s) in
        Darwin)
            for version in 21 22 23 24 25; do
                java_home=$(/usr/libexec/java_home -F -v "$version" 2>/dev/null || true)
                if usable_jdk "$java_home"; then break; fi
            done
            ;;
        Linux)
            javac_path=$(command -v javac || true)
            if [ -n "$javac_path" ]; then
                java_home=$(dirname "$(dirname "$(readlink -f "$javac_path")")")
            fi
            ;;
    esac

    if ! usable_jdk "$java_home"; then
        case $(uname -s) in
            Darwin) os=mac; jdk_suffix=/Contents/Home ;;
            Linux) os=linux; jdk_suffix='' ;;
            *) echo 'Use run.ps1 on Windows.' >&2; exit 2 ;;
        esac
        case $(uname -m) in
            arm64|aarch64) arch=aarch64 ;;
            x86_64) arch=x64 ;;
            *) echo 'A 64-bit JDK 21–25 is required.' >&2; exit 2 ;;
        esac
        platform="${os}_${arch}"
        install_dir="$project_dir/.tools/temurin-21-$platform"
        java_home="$install_dir$jdk_suffix"
        if ! usable_jdk "$java_home"; then
            manifest="$project_dir/tools/jdk-downloads.properties"
            url=$(sed -n "s/^${platform}_url=//p" "$manifest")
            checksum=$(sed -n "s/^${platform}_sha256=//p" "$manifest")
            mkdir -p "$project_dir/.tools"
            temp_dir=$(mktemp -d "$project_dir/.tools/jdk-download.XXXXXX")
            trap 'rm -rf "$temp_dir"' EXIT
            trap 'exit 130' HUP INT TERM

            echo 'Downloading Eclipse Temurin JDK 21 into .tools …'
            curl --fail --location --retry 2 --connect-timeout 15 --silent --show-error "$url" -o "$temp_dir/jdk.tar.gz"
            case $os in
                mac) checksum_line=$(LC_ALL=C shasum -a 256 "$temp_dir/jdk.tar.gz") ;;
                linux) checksum_line=$(sha256sum "$temp_dir/jdk.tar.gz") ;;
            esac
            actual_checksum=${checksum_line%% *}
            if [ "$actual_checksum" != "$checksum" ]; then
                echo 'The JDK archive checksum did not match.' >&2
                exit 1
            fi
            mkdir "$temp_dir/image"
            tar -xzf "$temp_dir/jdk.tar.gz" --strip-components 1 -C "$temp_dir/image"
            if ! usable_jdk "$temp_dir/image$jdk_suffix"; then
                echo 'The downloaded JDK cannot run on this computer.' >&2
                exit 1
            fi
            if [ ! -e "$install_dir" ]; then
                mv "$temp_dir/image" "$install_dir"
            fi
            if ! usable_jdk "$java_home"; then
                echo "Check the local JDK folder $install_dir and run this command again." >&2
                exit 1
            fi
            rm -rf "$temp_dir"
            trap - EXIT HUP INT TERM
        fi
    fi
    JAVA_HOME=$java_home
fi

export JAVA_HOME
exec "$JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 tools/TestLauncher.java "$@"
