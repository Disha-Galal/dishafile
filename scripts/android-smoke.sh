#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-results
adb install -r apk/app-debug.apk
adb install -r test-apk/app-debug-androidTest.apk
adb shell settings put global airplane_mode_on 1
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true || true
adb shell am instrument -w com.disha.filekit.test/androidx.test.runner.AndroidJUnitRunner | tee smoke-results/instrumentation.txt
adb logcat -d > smoke-results/logcat.txt
grep -q 'OK (' smoke-results/instrumentation.txt
! grep -q 'FAILURES\|INSTRUMENTATION_FAILED\|Process crashed' smoke-results/instrumentation.txt
