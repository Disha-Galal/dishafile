#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-results
adb install -r apk/app-debug.apk
adb install -r test-apk/app-debug-androidTest.apk
adb shell cmd connectivity airplane-mode enable
adb shell svc wifi disable
adb shell svc data disable
adb shell cmd connectivity airplane-mode | tee smoke-results/airplane-mode.txt
grep -q 'enabled' smoke-results/airplane-mode.txt
adb shell dumpsys connectivity > smoke-results/connectivity.txt
adb shell am instrument -w com.disha.filekit.test/androidx.test.runner.AndroidJUnitRunner | tee smoke-results/instrumentation.txt
adb logcat -d > smoke-results/logcat.txt
grep -q 'OK (' smoke-results/instrumentation.txt
! grep -q 'FAILURES\|INSTRUMENTATION_FAILED\|Process crashed' smoke-results/instrumentation.txt
