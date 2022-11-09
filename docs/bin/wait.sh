#!/bin/bash

# 等待任务
startTime=$(date +%s)
processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk '{print $2}')
if [ ! -z "$processId" ]; then
  waitIndex=0
  processPortNumber=$(netstat -anop | grep $processId | grep LISTEN | wc -l)
  while [ $waitIndex -le 120 ] && [ ! -z "$processId" ] && [ $processPortNumber -lt 1 ]
  do
    sleep 1
    waitIndex=$((waitIndex+1))
    processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk '{print $2}')
    if [ ! -z "$processId" ]; then
      processPortNumber=$(netstat -anop | grep $processId | grep LISTEN | wc -l)
    else
      processPortNumber=0
    fi
    echo -n "."
  done
  echo ""
fi
if [ $processPortNumber -lt 1 ]; then
  echo -e "\033[31m启动失败：${project.artifactId}-${project.version}\033[0m"
  sh bin/stop.sh
  exit 0
else
  finishTime=$(date +%s)
  processTime=$((finishTime-startTime))
  echo -e "\033[32m启动成功：${project.artifactId}-${project.version} - $processId\033[0m"
  echo "启动端口：$(netstat -anop | grep $processId | grep LISTEN | awk '{print $4}')"
  echo "启动耗时：$processTime S"
fi
