#!/bin/bash
cd "$(dirname "$0")"
echo "Compiling CFB Rankings website..."
javac FbsSchedules2026Compact.java FbsScheduleLoader.java Ranker.java CfbWebServer.java
if [ $? -ne 0 ]; then
  echo "Compilation failed. Check the error messages above."
  read -r -p "Press Return to close..."
  exit 1
fi
echo
echo "Starting the website at http://localhost:18765"
echo "Keep this Terminal window open while using the website."
echo "Press Control+C to stop the server."
java CfbWebServer
