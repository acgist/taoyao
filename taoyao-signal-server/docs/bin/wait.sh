#!/bin/bash

#########################
#        等待任务        #
#########################

# 休眠一秒：等待后台任务调度
sleep 2
aTime=$(date +%s)
processCt=0
processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk "{print $2}")
if [ ! -z "$processId" ]; then
  echo "等待应用：${project.artifactId}-${project.version} - $processId"
  waitIndex=0
  processCt=$(netstat -anop | grep $processId | grep LISTEN | wc -l)
  while [ ! -z "$processId" ] && [ $processCt -lt 1 ] && [ $waitIndex -le 120 ]
  do
    sleep 1
    waitIndex=$((waitIndex+1))
    processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk "{print $2}")
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
  cTime=$((finishTime-aTime))
  echo -e "\033[32m启动成功：${project.artifactId}-${project.version} - $processId\033[0m"
  echo "启动端口：$(netstat -anop | grep $processId | grep LISTEN | awk "{print $4}")"
  echo "启动耗时：$cTime S"
fi
