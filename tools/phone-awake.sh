#!/bin/sh
# Temporarily suspend display timeout for device development. No keyguard bypass.
set -eu
cd "$(dirname "$0")/.."
export RISH_APPLICATION_ID=com.termux
# USB development on a host, or authorized Shizuku when running on the phone.
phone_shell() {
    if command -v adb >/dev/null 2>&1 && adb get-state >/dev/null 2>&1; then
        adb shell "$1"
    else
        rish -c "$1"
    fi
}
saved=out/phone-screen-timeout-before.txt
case "${1:-status}" in
    on)
        mkdir -p out
        if [ ! -f "$saved" ]; then
            original=$(phone_shell 'settings get system screen_off_timeout' 2>&1)
            case "$original" in null) ;; ''|*[!0-9]*) echo 'Could not read screen timeout' >&2; exit 1;; esac
            printf '%s\n' "$original" > "$saved"
        fi
        actual=$(phone_shell 'settings put system screen_off_timeout 2147483647
input keyevent KEYCODE_WAKEUP
settings get system screen_off_timeout' 2>&1)
        if [ "$actual" != 2147483647 ]; then echo 'Could not enable awake mode' >&2; exit 1; fi
        echo 'Display timeout suspended; original setting saved.'
        ;;
    off)
        if [ ! -f "$saved" ]; then echo 'No temporary timeout to restore.'; exit 0; fi
        original=$(cat "$saved")
        case "$original" in
            null) phone_shell 'settings delete system screen_off_timeout' ;;
            ''|*[!0-9]*) echo 'Invalid saved timeout; restore manually' >&2; exit 1;;
            *) phone_shell "settings put system screen_off_timeout $original" ;;
        esac
        actual=$(phone_shell 'settings get system screen_off_timeout' 2>&1)
        if [ "$actual" != "$original" ]; then echo 'Timeout restoration failed; saved setting retained' >&2; exit 1; fi
        rm "$saved"
        printf 'Restored screen timeout: %s ms\n' "$original"
        ;;
    status) phone_shell 'settings get system screen_off_timeout' ;;
    *) echo 'Usage: tools/phone-awake.sh on|off|status' >&2; exit 2;;
esac
