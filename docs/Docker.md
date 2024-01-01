# Docker

## 镜像

```
# Web
docker pull acgist/taoyao-client-web:1.0.0
docker run --name="taoyao-client-web" -d acgist/taoyao-client-web:1.0.0
docker run --name="taoyao-client-web" -d -p 0.0.0.0:8443:8443 acgist/taoyao-client-web:1.0.0
docker run --name="taoyao-client-web" -it acgist/taoyao-client-web:1.0.0 /bin/bash
docker exec -it acgist/taoyao-client-web:1.0.0 /bin/bash

# 媒体服务
docker pull acgist/taoyao-client-media:1.0.0
docker run --name="taoyao-client-media" -d acgist/taoyao-client-media:1.0.0
docker run --name="taoyao-client-media" --net=host -d acgist/taoyao-client-media:1.0.0
docker run --name="taoyao-client-media" -it acgist/taoyao-client-media:1.0.0 /bin/bash
docker exec -it acgist/taoyao-client-media:1.0.0 /bin/bash

# 信令服务
docker pull acgist/taoyao-signal-server:1.0.0
docker run --name="taoyao-signal-server" -d acgist/taoyao-signal-server:1.0.0
docker run --name="taoyao-signal-server" -d -p 0.0.0.0:8888:8888 -p 0.0.0.0:9999:9999 acgist/taoyao-signal-server:1.0.0
docker run --name="taoyao-signal-server" -it acgist/taoyao-signal-server:1.0.0 /bin/bash
docker exec -it acgist/taoyao-signal-server:1.0.0 /bin/bash
```

## 制作

```
# Web
docker image build -t acgist/taoyao-client-web:1.0.0 .
docker push acgist/taoyao-client-web:1.0.0

# 媒体服务
docker image build -t acgist/taoyao-client-media:1.0.0 .
docker push acgist/taoyao-client-media:1.0.0

# 信令服务
docker image build -t acgist/taoyao-signal-server:1.0.0 .
docker push acgist/taoyao-signal-server:1.0.0
```

## Dockerfile

### Web

```
FROM node:18.16.0
EXPOSE 8443/tcp
COPY taoyao-client-web /data/taoyao/taoyao-client-web
WORKDIR /data/taoyao/taoyao-client-web
CMD npm run dev
```

### 媒体服务

```
FROM node:18.16.0
COPY taoyao-client-media /data/taoyao/taoyao-client-media
WORKDIR /data/taoyao/taoyao-client-media
CMD npm run dev
```

### 信令服务

```
FROM openjdk:17.0.2-jdk
EXPOSE 8888/tcp
EXPOSE 9999/tcp
COPY taoyao-signal-server /data/taoyao/taoyao-signal-server
WORKDIR /data/taoyao/taoyao-signal-server
CMD ./deploy/bin/startup.sh
```

## DockerCompose

注意需要自己配置媒体服务（修改IP地址）

```
version: "3.1"

services:

  taoyao-client-web:
    image: acgist/taoyao-client-web:1.0.0
    restart: always
    privileged: true
    network_mode: host
    container_name: taoyao-client-web
    volumes:
      - /etc/localtime:/etc/localtime:ro
    environment:
      - TZ=Asia/Shanghai

  taoyao-client-media:
    image: acgist/taoyao-client-media:1.0.0
    restart: always
    privileged: true
    network_mode: host
    container_name: taoyao-client-media
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - ./taoyao-client-media/Config.js:/data/taoyao/taoyao-client-media/src/Config.js
    environment:
      - TZ=Asia/Shanghai

  taoyao-signal-server:
    image: acgist/taoyao-signal-server:1.0.0
    restart: always
    privileged: true
    network_mode: host
    container_name: taoyao-signal-server
    volumes:
      - /etc/localtime:/etc/localtime:ro
    environment:
      - TZ=Asia/Shanghai
```
