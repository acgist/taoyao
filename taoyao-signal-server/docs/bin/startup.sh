#!/bin/bash

#########################
#        开始任务        #
#########################

# 启动目录
bin=$(readlink -f $(dirname $0))
base=${bin%/*}
cd $base
echo "环境目录：$base"
echo "启动目录：$(pwd)"

# Java运行环境
if [ ! -f "/.dockerenv" ]; then
  JAVA=$(which java)
  if [ -z "$JAVA" ] ; then
    echo "必须安装${java.version}+JDK"
    exit 1
  fi
  # 结束任务
  bash bin/stop.sh
else
  JAVA="java"
fi

# 启动参数
JAVA_OPTS_GC="-XX:+UseG1GC -Xlog:gc:./logs/gc.log:time,level"
JAVA_OPTS_MEM="-server ${taoyao.maven.jvm.mem}"
JAVA_OPTS_EXT="-Dfile.encoding=${taoyao.maven.encoding} -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true"
JAVA_OPTS_APP="-Dspring.profiles.active=${profile}"
JAVA_OPTS="$JAVA_OPTS_MEM $JAVA_OPTS_EXT $JAVA_OPTS_APP ${taoyao.maven.jvm.arg}"
echo "启动参数：$JAVA_OPTS"

# 启动应用
echo "启动应用：${project.artifactId}-${project.version}"
if [ ! -f "/.dockerenv" ]; then
  nohup $JAVA $JAVA_OPTS -jar $base/lib/${project.artifactId}-${project.version}.jar > /dev/null 2>&1 &
else
  $JAVA $JAVA_OPTS -jar $base/lib/${project.artifactId}-${project.version}.jar
fi

# 等待任务
if [ ! -f "/.dockerenv" ]; then
  bash bin/wait.sh
else
  echo -e "\033[32m启动成功：${project.artifactId}-${project.version}\033[0m"
fi

echo "--------------------------------"
