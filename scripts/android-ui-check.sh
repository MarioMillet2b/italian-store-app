#!/usr/bin/env bash
# Isolated Android emulator only; the instrumentation gateway is synthetic.
set -euo pipefail
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
: "${sdk_root:?Android SDK is required}"
adb_bin="$sdk_root/platform-tools/adb"
apk_root="${FLORO_UI_APK_DIR:-app/build}"
output_dir="${FLORO_UI_OUTPUT:-app/build/reports/ui}"
mkdir -p "$output_dir"
serial="${FLORO_UI_SERIAL:-emulator-5554}"
if [[ "$serial" != emulator-* ]]; then
    echo 'UI fixtures may only run on an emulator.' >&2
    exit 1
fi
emulator_pid=''
avd_home=''
cleanup() {
    if [[ -n "$emulator_pid" ]]; then kill "$emulator_pid" 2>/dev/null || true; fi
}
trap cleanup EXIT
if [[ -z "${FLORO_UI_SERIAL:-}" ]]; then
    [[ -r /dev/kvm && -w /dev/kvm ]] || { echo 'CI visual tests require KVM.' >&2; exit 1; }
    api="${FLORO_UI_API:-29}"
    tag="${FLORO_UI_TAG:-default}"
    [[ "$api" == 29 || "$api" == 35 ]]
    [[ "$tag" == default || "$tag" == google_apis ]]
    avd_home="$(mktemp -d)"
    export ANDROID_AVD_HOME="$avd_home"
    printf 'no\n' | "$(command -v avdmanager)" create avd -n floro-ui -k "system-images;android-$api;$tag;x86_64" -d pixel_6a
    "$sdk_root/emulator/emulator" -avd floro-ui -port 5554 -no-window -no-audio -no-boot-anim -no-snapshot -accel on -gpu swiftshader -memory 2048 -no-metrics > "$output_dir/emulator.log" 2>&1 &
    emulator_pid=$!
fi
ready=false
for attempt in $(seq 1 90); do
    if [[ "$(timeout 5 "$adb_bin" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then ready=true; break; fi
    sleep 2
done
[[ "$ready" == true ]] || { echo 'Emulator did not finish booting.' >&2; exit 1; }
main_apk="$(find "$apk_root" -type f -name app-debug.apk)"
test_apk="$(find "$apk_root" -type f -name app-debug-androidTest.apk)"
[[ -f "$main_apk" && -f "$test_apk" ]]
# A CI APK may have another debug signature; no real shop data lives here.
"$adb_bin" -s "$serial" uninstall com.floromilano.app.test >/dev/null 2>&1 || true
"$adb_bin" -s "$serial" uninstall com.floromilano.app >/dev/null 2>&1 || true
"$adb_bin" -s "$serial" install "$main_apk"
"$adb_bin" -s "$serial" install "$test_apk"
for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
    "$adb_bin" -s "$serial" shell settings put global "$setting" 0
done
"$adb_bin" -s "$serial" shell input keyevent 82
"$adb_bin" -s "$serial" shell am instrument -w -e class com.floromilano.app.UiExperienceTest com.floromilano.app.test/androidx.test.runner.AndroidJUnitRunner > "$output_dir/instrumentation.log" 2>&1
"$adb_bin" -s "$serial" pull /sdcard/Android/data/com.floromilano.app/files/ux-audit "$output_dir/screenshots" || true
cat "$output_dir/instrumentation.log"
grep -Eq 'OK \([1-9][0-9]* tests?\)' "$output_dir/instrumentation.log"
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$output_dir/instrumentation.log"
