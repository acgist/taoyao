#!/bin/bash

#########################
#        等待任务        #
#########################

aTime=$(date +%s)
waitIndex=0
processCt=0
processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk '{print $2}')
while [ -z "$processId" ] && [ $waitIndex -le 8 ]
do
  echo "等待调度：${project.artifactId}-${project.version}"
  sleep 1
  waitIndex=$((waitIndex+1))
  processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk '{print $2}')
done
if [ ! -z "$processId" ]; then
  echo "等待启动：${project.artifactId}-${project.version} - $processId"
  processCt=$(netstat -anop | grep $processId | grep LISTEN | wc -l)
  while [ ! -z "$processId" ] && [ $processCt -lt 1 ] && [ $waitIndex -le 120 ]
  do
    sleep 1
    waitIndex=$((waitIndex+1))
    processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk '{print $2}')
    if [ ! -z "$processId" ]; then
      processCt=$(netstat -anop | grep $processId | grep LISTEN | wc -l)
    else
      processCt=0
    fi
    echo -n "."
  done
  echo ""
fi
if [ $processCt -lt 1 ]; then
  echo -e "\033[31m启动失败：${project.artifactId}-${project.version} - $processId\033[0m"
  bash bin/stop.sh
  exit 0
else
  zTime=$(date +%s)
  processTime=$((zTime-aTime))
  echo -e "\033[32m启动成功：${project.artifactId}-${project.version} - $processId\033[0m"
  echo "启动端口：$(netstat -anop | grep $processId | grep LISTEN | awk '{print $4}')"
  echo "启动耗时：$processTime S"
fi
