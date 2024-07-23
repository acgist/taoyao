# RNNOISE模型

https://github.com/xiph/rnnoise.git

## FFmpeg

```
ffplay -ar 48000 -ac 1 -f s16le input.pcm
ffmpeg.exe -i .\source.wav    -ar 48000 -ac 1 -f s16le -c:a pcm_s16le source.raw
ffmpeg.exe -i .\source.ts -vn -ar 48000 -ac 1 -f s16le -c:a pcm_s16le source.raw
```

## 环境

```
sudo apt install pip3 python3

vim ~/.pip/pip.conf

---
[global]
index-url=https://pypi.tuna.tsinghua.edu.cn/simple
---

pip3 install tqdm torch
```

## 训练

训练音频`48000`采样单声道的`PCM`音频数据

```
# 混合数据：mix.pcm
# 噪音数据：noise.pcm
# 原始数据：speech.pcm

# 克隆仓库
cd /data
git clone http://192.168.8.184:9999/dev/hsx/rnnoise.git
cd rnnoise

# 编译代码
./autogen.sh
./configure
make

# 提取特征
./dump_features speech.pcm noise.pcm features.f32 200000
./script/dump_features_parallel.sh ./dump_features speech.pcm noise.pcm features.f32 200000 8
# 模型训练
python3 train_rnnoise.py --gru-size=32 --cond-size=32 --epochs=15 features.f32 ./
# 导出权重
python3 dump_rnnoise_weights.py --quantize ./checkpoints/rnnoise_1.pth rnnoise_c

# 验证效果
./examples/rnnoise_demo mix.pcm output.pcm
```
