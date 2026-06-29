#!/bin/sh
set -eu

MODE="${MODE:-heap-oom}"
THREADS="${THREADS:-300}"

JAVA_XMS="${JAVA_XMS:-64m}"
JAVA_XMX="${JAVA_XMX:-128m}"
JFR_SETTINGS="${JFR_SETTINGS:-profile}"
NMT="${NMT:-true}"

TZ="${TZ:-Asia/Taipei}"
JAVA_TIMEZONE="${JAVA_TIMEZONE:-Asia/Taipei}"
export TZ

mkdir -p /workspace/dumps /workspace/logs /workspace/jfr

echo "===== Container cgroup info ====="

if [ -f /sys/fs/cgroup/memory.max ]; then
  echo "memory.max = $(cat /sys/fs/cgroup/memory.max)"
fi

if [ -f /sys/fs/cgroup/cpu.max ]; then
  echo "cpu.max = $(cat /sys/fs/cgroup/cpu.max)"
fi

echo "===== Timezone info ====="
echo "TZ=$TZ"
echo "JAVA_TIMEZONE=$JAVA_TIMEZONE"
date

echo "===== JVM Lab starting ====="
echo "MODE=$MODE"
echo "THREADS=$THREADS"
echo "JAVA_XMS=$JAVA_XMS"
echo "JAVA_XMX=$JAVA_XMX"
echo "JFR_SETTINGS=$JFR_SETTINGS"
echo "NMT=$NMT"

NMT_ARG=""
if [ "$NMT" = "true" ]; then
  NMT_ARG="-XX:NativeMemoryTracking=summary"
fi

exec java \
  $NMT_ARG \
  "-Duser.timezone=${JAVA_TIMEZONE}" \
  "-Xms${JAVA_XMS}" \
  "-Xmx${JAVA_XMX}" \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  "-XX:HeapDumpPath=/workspace/dumps" \
  -XX:+ExitOnOutOfMemoryError \
  "-Xlog:gc*:file=/workspace/logs/gc-${MODE}.log:time,uptime,level,tags:filecount=5,filesize=10m" \
  "-XX:StartFlightRecording=name=${MODE},settings=${JFR_SETTINGS},disk=true,maxsize=256m,maxage=1h,dumponexit=true,filename=/workspace/jfr/${MODE}-%p-%t.jfr" \
  -cp /app/classes \
  JvmLab "$MODE" "$THREADS"