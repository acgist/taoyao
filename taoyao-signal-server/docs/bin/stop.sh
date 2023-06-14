#!/bin/bash

#########################
#        结束任务        #
#########################

killIndex=0
processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk '{print $2}')
if [ ! -z "$processId" ]; then
  echo "关闭应用：${project.artifactId}-${project.version} - $processId"
  while [ ! -z "$processId" ]
  do
    echo -n "."
    if [ $killIndex -le 0 ]; then
      # 优雅关机
      kill -15 $processId
    elif [ $killIndex -ge 10 ]; then
      # 强制关机
      echo -n "强制关闭"
      kill -9 $processId
    fi
    sleep 1
    killIndex=$((killIndex+1))
    processId=$(ps -aux | grep "${project.artifactId}" | grep java | awk '{print $2}')
  done
  echo ""
fi
