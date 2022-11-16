# FFmpeg

默认使用`ffmpeg-platform`所以不用安装，如果使用本地FFmpeg需要自己安装。

## FFmpeg

```
# nasm
wget https://www.nasm.us/pub/nasm/releasebuilds/2.14/nasm-2.14.tar.gz
tar zxvf nasm-2.14.tar.gz
cd nasm-2.14
./configure --prefix=/usr/local/nasm
make -j && make install
# 环境变量
vim /etc/profile
export PATH=$PATH:/usr/local/nasm/bin
source /etc/profile

# yasm
wget http://www.tortall.net/projects/yasm/releases/yasm-1.3.0.tar.gz
tar zxvf yasm-1.3.0.tar.gz
cd yasm-1.3.0
./configure --prefix=/usr/local/yasm
make -j && make install
# 环境变量
vim /etc/profile
export PATH=$PATH:/usr/local/yasm/bin
source /etc/profile

# x264
git clone https://code.videolan.org/videolan/x264.git
cd x264
./configure --prefix=/usr/local/x264 --libdir=/usr/local/lib --includedir=/usr/local/include --enable-shared --enable-static
make -j && make install
# 环境变量
vim /etc/profile
export PATH=$PATH:/usr/local/x264/bin
source /etc/profile

# 编码解码
# acc
https://github.com/mstorsjo/fdk-aac.git
--enable-libfdk_aac
# vpx
https://github.com/webmproject/libvpx.git
--enable-libvpx
# x265
https://bitbucket.org/multicoreware/x265
--enable-libx265
# opus
https://archive.mozilla.org/pub/opus/opus-1.2.1.tar.gz
--enable-libopus

# ffmpeg
wget http://www.ffmpeg.org/releases/ffmpeg-4.3.1.tar.xz
tar xvJf ffmpeg-4.3.1.tar.xz
cd ffmpeg-4.3.1
./configure --prefix=/usr/local/ffmpeg --enable-gpl --enable-shared --enable-libx264
# --enable-cuda --enable-cuvid --enable-nvenc --nvcc=/usr/local/cuda-11.0/bin/nvcc
make -j && make install
# 环境变量
vim /etc/profile
export PATH=$PATH:/usr/local/ffmpeg/bin
source /etc/profile

# lib
vim /etc/ld.so.conf
/usr/local/x264/lib/
/usr/local/ffmpeg/lib/
ldconfig

# 查看版本
ffmpeg -version
# 查看编解码
ffmpeg -codecs
# 格式化文件
ffmpeg -y -i source.mkv -c copy target.mp4
# 查看文件格式
ffprobe -v error -show_streams -print_format json source.mp4
```

## GPU

```
驱动
# cuda
https://developer.nvidia.com/cuda-downloads
# nv-codec-headers
https://git.videolan.org/git/ffmpeg/nv-codec-headers.git
# 验证
nvidia-smi
```
