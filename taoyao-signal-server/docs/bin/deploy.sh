#!/bin/bash

#########################
#        部署任务        #
#########################

# 进入目录
base=$(readlink -f $(dirname $0))
cd $base
echo "环境目录：$base"
echo "当前目录：$(pwd)"

# 更新代码
echo "更新代码：${project.artifactId}-${project.version}"
git pull

# 编译代码
echo "编译代码：${project.artifactId}-${project.version}"
mvn clean package -D skipTests -P ${profile}
# 编译指定模块以及依赖
# mvn clean package -pl "${project.groupId}:${project.artifactId}" -am -D skipTests -P ${profile}

# 删除文件
rm -rf $base/deploy/bin
rm -rf $base/deploy/lib
rm -rf $base/deploy/config

# 运行目录
echo "拷贝文件：${project.artifactId}-${project.version}"
if [ ! -d "$base/deploy" ]; then
  mkdir -p $base/deploy
fi

# 拷贝文件
cp -rf ${project.basedir}/target/${project.artifactId}-${project.version}/* $base/deploy

# 启动服务
echo "启动项目：${project.artifactId}-${project.version}"
sudo systemctl restart taoyao-signal-server
sudo systemctl status  taoyao-signal-server
