#!/bin/bash

# 进入目录
base=$(readlink -f $(dirname $0))
cd $base
echo "环境目录：$base"
echo "当前目录：$(pwd)"

# 更新代码
if [ -z $gited ]; then
  echo "更新代码：${project.artifactId}-${project.version}"
  git pull
fi

# 编译代码
if [ -z $mvned ]; then
  echo "编译代码：${project.artifactId}-${project.version}"
  mvn clean package install -pl "${project.groupId}:${project.artifactId}" -am -D skipTests -P ${env}
fi

# 删除文件：注意不要删除日志
rm -rf $base/../deploy/${project.artifactId}/bin
rm -rf $base/../deploy/${project.artifactId}/lib
rm -rf $base/../deploy/${project.artifactId}/config
# 运行目录
echo "拷贝文件：${project.artifactId}-${project.version}"
if [ ! -d "$base/../deploy/${project.artifactId}" ]; then
  mkdir -p $base/../deploy/${project.artifactId}
fi
# 拷贝文件
cp -rf ${project.basedir}/target/${project.artifactId}-${project.version}/* $base/../deploy/${project.artifactId}
cp -rf ${project.basedir}/target/${project.artifactId}-${project.version}.jar $base/../deploy/${project.artifactId}

# 启动服务
cd $base/../deploy/${project.artifactId}
sh bin/startup.sh
