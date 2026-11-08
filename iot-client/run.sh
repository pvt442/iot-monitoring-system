#!/usr/bin/env bash
# IoT Client Simulator - biên dịch và chạy (chỉ cần JDK 21, không cần Maven)
# Ví dụ:  ./run.sh --server=http://localhost:8080 --interval=3000 --devices=DEV_01,DEV_02
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
exec java -Dfile.encoding=UTF-8 -cp out com.demo.client.ClientApp "$@"
