/** 桃夭WebRTC终端核心功能 */
/** 配置 */
const config = {
	// 当前终端SN
	sn: 'taoyao',
	// 信令授权
	username: 'taoyao',
	password: 'taoyao'
};
/** 音频配置 */
const defaultAudioConfig = {
	// 音量：0~1
	volume: 0.5,
	// 设备
	// deviceId : '',
	// 采样率：8000|16000|32000|48000
	sampleRate: 32000,
	// 采样数：16
	sampleSize: 16,
	// 延迟大小（单位毫秒）：500毫秒以内较好
	latency: 0.3,
	// 声道数量：1|2
	channelCount : 1,
	// 是否开启自动增益：true|false
	autoGainControl: false,
	// 是否开启降噪功能：true|false
	noiseSuppression: true,
	// 是否开启回音消除：true|false
	echoCancellation: true,
	// 消除回音方式：system|browser
	echoCancellationType: 'system'
};
/** 视频配置 */
const defaultVideoConfig = {
	// 宽度
	width: 1280,
	// 高度
	height: 720,
	// 设备
	// deviceId: '', 
	// 帧率
	frameRate: 24,
	// 裁切
	// resizeMode: '',
	// 选摄像头：user|left|right|environment 
	facingMode: 'environment'
}
/** 兼容 */
const XMLHttpRequest = window.XMLHttpRequest;
const PeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
/** 桃夭 */
function Taoyao(
	webSocket,
	iceServer,
	audioConfig,
	videoConfig
) {
	this.webSocket = webSocket;
	this.iceServer = iceServer;
	/** 媒体状态 */
	this.audioStatus = true;
	this.videoStatus = true;
	/** 设备状态 */
	this.audioEnabled = true;
	this.videoEnabled = true;
	/** 媒体信息 */
	this.audioStreamId = null;
	this.videoStreamId = null;
	/** 媒体配置 */
	this.audioConfig = audioConfig || defaultAudioConfig;
	this.videoConfig = videoConfig || defaultVideoConfig;
	/** 本地视频 */
	this.localVideo = null;
	/** 终端媒体 */
	this.clientMedia = {};
	/** 信令通道 */
	this.signalChannel = null;
	/** 发送信令 */
	this.push = null;
	/** 检查设备 */
	this.checkDevice = function() {
		let self = this;
		if(navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {
			navigator.mediaDevices.enumerateDevices()
			.then(list => {
				let audioDevice = false;
				let videoDevice = false;
				list.forEach(v => {
					console.log('终端媒体设备', v.kind, v.label);
					if(v.kind === 'audioinput') {
						audioDevice = true;
					} else if(v.kind === 'videoinput') {
						videoDevice = true;
					}
				});
				if(!audioDevice) {
					console.log('终端没有音频输入设备');
					self.audioEnabled = false;
				}
				if(!videoDevice) {
					console.log('终端没有视频输入设备');
					self.videoEnabled = false;
				}
			})
			.catch(e => {
				console.log('获取终端设备失败', e);
				self.videoEnabled = false;
				self.videoEnabled = false;
			});
		}
		return this;
	};
	/** 请求 */
	this.request = function(url, data = {}, method = 'GET', async = true, timeout = 5000, mime = 'json') {
		return new Promise((resolve, reject) => {
			let xhr = new XMLHttpRequest();
			xhr.open(method, url, async);
			if(async) {
				xhr.timeout = timeout;
				xhr.responseType = mime;
				xhr.send(data);
				xhr.onload = function() {
					if(xhr.readyState === 4 && xhr.status === 200) {
						resolve(xhr.response);
					} else {
						reject(xhr.response);
					}
				}
				xhr.onerror = reject;
			} else {
				xhr.send(data);
				if(xhr.readyState === 4 && xhr.status === 200) {
					resolve(JSON.parse(xhr.response));
				} else {
					reject(JSON.parse(xhr.response));
				}
			}
		});
	};
	/** 媒体配置 */
	this.configMedia = function(audio = {}, video = {}) {
		this.audioConfig = {...this.audioConfig, ...audio};
		this.videoCofnig = {...this.videoCofnig, ...video};
		console.log('终端媒体配置', this.audioConfig, this.videoConfig);
	};
	/** WebRTC配置 */
	this.configWebrtc = function(config = {}) {
		this.webSocket = config.signalAddress;
		this.iceServer = config.stun;
		console.log('WebRTC配置', this.webSocket, this.iceServer);
	};
	/** 信令通道 */
	this.buildChannel = function(callback) {
		this.signalChannel = signalChannel;
		this.signalChannel.connect(this.webSocket, callback);
		// 不能直接this.push = this.signalChannel.push这样导致this对象错误
		this.push = function(data, callback) {
			this.signalChannel.push(data, callback)
		};
	};
	/** 本地媒体 */
	this.buildLocalMedia = function() {
		console.log("获取终端媒体：", this.audioConfig, this.videoConfig);
		let self = this;
		return new Promise((resolve, reject) => {
			if(navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
				navigator.mediaDevices.getUserMedia({
					audio: self.audioConfig,
					video: self.videoConfig
				})
				.then(resolve)
				.catch(reject);
			} else if(navigator.getUserMedia) {
				navigator.getUserMedia({
					audio: self.audioConfig,
					video: self.videoConfig
				}, resolve, reject);
			} else {
				reject("获取终端媒体失败");
			}
		});
	};
	/** 本地媒体 */
	this.localMedia = async function(localVideoId, stream) {
		this.localVideo = document.getElementById(localVideoId);
		if ('srcObject' in this.localVideo) {
			this.localVideo.srcObject = stream;
		} else {
			this.localVideo.src = URL.createObjectURL(stream);;
		}
		await this.localVideo.play();
	};
	/** 媒体 */
	/** 视频 */
};
/** 信令协议 */
const protocol = {
	pid: {
		/** 心跳 */
		heartbeat: 1000,
		/** 注册 */
		register:  2000
	},
	/** 当前索引 */
	index: 100000,
	/** 最小索引 */
	minIndex: 100000,
	/** 最大索引 */
	maxIndex: 999999,
	/** 生成ID */
	buildId: function() {
		if(this.index++ >= this.maxIndex) {
			this.index = this.minIndex;
		}
		return Date.now() + '' + this.index;
	},
	/** 生成协议 */
	buildProtocol: function(sn, pid, body) {
		let message = {
			header: {
				v: '1.0.0',
				id: this.buildId(),
				sn: sn,
				pid: pid,
			},
			"body": body
		};
		return message;
	}
};
/** 信令消息 */
/** 信令通道 */
const signalChannel = {
	/** 通道 */
	channel: null,
	/** 地址 */
	address: null,
	/** 回调 */
	callback: null,
	/** 回调事件 */
	callbackMapping: new Map(),
	/** 心跳时间 */
	heartbeatTime: 10 * 1000,
	/** 心跳定时器 */
	heartbeatTimer: null,
	/** 防止重连 */
	lockReconnect: false,
	/** 重连时间 */
	connectionTimeout: 5 * 1000,
	/** 最小重连时间 */
	minReconnectionDelay: 5 * 1000,
	/** 最大重连时间 */
	maxReconnectionDelay: 5 * 60 * 1000,
	/** 自动重连失败后重连时间增长倍数 */
	reconnectionDelayGrowFactor: 1.5,
	/** 关闭 */
	close: function() {
		clearTimeout(this.heartbeatTimer);
	},
	/** 心跳 */
	heartbeat: function() {
		let self = this;
		self.heartbeatTimer = setTimeout(function() {
			if (self.channel && self.channel.readyState == WebSocket.OPEN) {
				self.push(protocol.buildProtocol(config.sn, protocol.pid.heartbeat));
				self.heartbeat();
			} else {
				console.log('发送心跳失败', self.channel);
			}
		}, self.heartbeatTime);
	},
	/** 重连 */
	reconnect: function() {
		let self = this;
		if (self.lockReconnect) {
			return;
		}
		self.lockReconnect = true;
		// 关闭旧的通道
		if(self.channel && self.channel.readyState == WebSocket.OPEN) {
			self.channel.close();
			self.channel = null;
		}
		// 打开定时重连
		setTimeout(function() {
			console.log('信令通道重连', self.address);
			self.connect(self.address, self.callback, true);
			self.lockReconnect = false;
		}, self.connectionTimeout);
		if (self.connectionTimeout >= self.maxReconnectionDelay) {
			self.connectionTimeout = self.maxReconnectionDelay;
		} else {
			self.connectionTimeout = self.connectionTimeout * self.reconnectionDelayGrowFactor
		}
	},
	/** 连接 */
	connect: function(address, callback, reconnection = true) {
		let self = this;
		this.address = address;
		this.callback = callback;
		console.log("连接信令通道", address);
		return new Promise((resolve, reject) => {
			self.channel = new WebSocket(address);
			self.channel.onopen = function(e) {
				console.log('信令通道打开', e);
				self.push(protocol.buildProtocol(
					config.sn,
					protocol.pid.register,
					{
						ip: null,
						mac: null,
						signal: 100,
						battery: 100,
						username: config.username,
						password: config.password
					}
				));
				self.connectionTimeout = self.minReconnectionDelay
				self.heartbeat();
				resolve(e);
			};
			self.channel.onclose = function(e) {
				console.log('信令通道关闭', self.channel, e);
				if(reconnection) {
					self.reconnect();
				}
				reject(e);
			};
			self.channel.onerror = function(e) {
				console.error('信令通道异常', self.channel, e);
				if(reconnection) {
					self.reconnect();
				}
				reject(e);
			};
			self.channel.onmessage = function(e) {
				console.log('信令消息', e.data);
				let data = JSON.parse(e.data);
				// 注册回调
				if(callback) {
					callback(data);
				}
				// 请求回调
				if(self.callbackMapping.has(data.header.id)) {
					self.callbackMapping.get(data.header.id)();
					self.callbackMapping.delete(data.header.id);
				}
			};
		});
	},
	/** 信令消息 */
	push: function(data, callback) {
		// 注册回调
		if(data && callback) {
			this.callbackMapping.set(data.header.id, callback);
		}
		if(data && data.header) {
			this.channel.send(JSON.stringify(data));
		} else {
			this.channel.send(data);
		}
	}
};
/*
var peer;
var socket; // WebSocket
var supportStream = false; // 是否支持使用数据流
var localVideo; // 本地视频
var localVideoStream; // 本地视频流
var remoteVideo; // 远程视频
var remoteVideoStream; // 远程视频流
var initiator = false; // 是否已经有人在等待
var started = false; // 是否开始
var channelReady = false; // 是否打开WebSocket通道
// 初始
function initialize() {
	console.log("初始聊天");
	// 获取视频
	localVideo = document.getElementById("localVideo");
	remoteVideo = document.getElementById("remoteVideo");
	supportStream = "srcObject" in localVideo;
	// 显示状态
	if (initiator) {
		setNotice("开始连接");
	} else {
		setNotice("加入聊天：https://www.acgist.com/demo/video/?oid=FFB85D84AC56DAF88B7E22AFFA7533D3");
	}
	// 打开WebSocket
	openChannel();
	// 创建终端媒体
	buildUserMedia();
}
function openChannel() {
	console.log("打开WebSocket");
	socket = new WebSocket("wss://www.acgist.com/video.ws/FFB85D84AC56DAF88B7E22AFFA7533D3");
	socket.onopen = channelOpened;
	socket.onmessage = channelMessage;
	socket.onclose = channelClosed;
	socket.onerror = channelError;
}
function channelOpened() {
	console.log("打开WebSocket成功");
	channelReady = true;
}
function channelMessage(message) {
	console.log("收到消息：" + message.data);
	var msg = JSON.parse(message.data);
	if (msg.type === "offer") { // 处理Offer消息
		if (!initiator && !started) {
			connectPeer();
		}
		peer.setRemoteDescription(new RTCSessionDescription(msg));
		peer.createAnswer().then(buildLocalDescription);
	} else if (msg.type === "answer" && started) { // 处理Answer消息
		peer.setRemoteDescription(new RTCSessionDescription(msg));
	} else if (msg.type === "candidate" && started) {
		var candidate = new RTCIceCandidate({
			sdpMLineIndex : msg.label,
			candidate : msg.candidate
		});
		peer.addIceCandidate(candidate);
	} else if (msg.type === "bye" && started) {
		onRemoteClose();
		setNotice("对方已断开！");
	} else if(msg.type === "nowaiting") {
		onRemoteClose();
		setNotice("对方已离开！");
	}
}
function channelClosed() {
	console.log("关闭WebSocket");
	openChannel(); // 重新打开WebSocket
}
function channelError(event) {
	console.log("WebSocket异常：" + event);
}
function buildUserMedia() {
	console.log("获取终端媒体");
	if(navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
		navigator.mediaDevices.getUserMedia({
			"audio" : true,
			"video" : true
		})
		.then(onUserMediaSuccess)
		.catch(onUserMediaError);
	} else {
		navigator.getUserMedia({
			"audio" : true,
			"video" : true
		}, onUserMediaSuccess, onUserMediaError);
	}
}
function onUserMediaSuccess(stream) {
	localVideoStream = stream;
	if (supportStream) {
		localVideo.srcObject = localVideoStream;
	} else {
		localVideo.src = URL.createObjectURL(localVideoStream);
	}
	if (initiator) {
		connectPeer();
	}
}
function onUserMediaError(error) {
	alert("请打开摄像头！");
}
function connectPeer() {
	if (!started && localVideoStream && channelReady) {
		console.log("开始连接Peer");
		started = true;
		buildPeerConnection();
		peer.addStream(localVideoStream);
		if (initiator) {
			peer.createOffer().then(buildLocalDescription);
		}
	}
}
function buildPeerConnection() {
	//var server = {"iceServers" : [{"url" : "stun:stun.l.google.com:19302"}]};
	var server = {"iceServers" : [{"url" : "stun:stun1.l.google.com:19302"}]};
	peer = new PeerConnection(server);
	peer.onicecandidate = peerIceCandidate;
	peer.onconnecting = peerConnecting;
	peer.onopen = peerOpened;
	peer.onaddstream = peerAddStream;
	peer.onremovestream = peerRemoveStream;
}
function peerIceCandidate(event) {
	if (event.candidate) {
		sendMessage({
			type : "candidate",
			id : event.candidate.sdpMid,
			label : event.candidate.sdpMLineIndex,
			candidate : event.candidate.candidate
		});
	} else {
		console.log("不支持的candidate");
	}
}
function peerConnecting(message) {
	console.log("Peer连接");
}
function peerOpened(message) {
	console.log("Peer打开");
}
function peerAddStream(event) {
	console.log("远程视频添加");
	remoteVideoStream = event.stream;
	if(supportStream) {
		remoteVideo.srcObject = remoteVideoStream;
	} else {
		remoteVideo.src = URL.createObjectURL(remoteVideoStream);
	}
	setNotice("连接成功");
	waitForRemoteVideo();
}
function peerRemoveStream(event) {
	console.log("远程视频移除");
}
function buildLocalDescription(description) {
	peer.setLocalDescription(description);
	sendMessage(description);
}
function sendMessage(message) {
	var msgJson = JSON.stringify(message);
	socket.send(msgJson);
	console.log("发送信息：" + msgJson);
}
function setNotice(msg) {
	document.getElementById("footer").innerHTML = msg;
}
function onRemoteClose() {
	started = false;
	initiator = false;
	if(supportStream) {
		remoteVideo.srcObject = null;
	} else {
		remoteVideo.src = null;
	}
	peer.close();
}
function waitForRemoteVideo() {
	if (remoteVideo.currentTime > 0) { // 判断远程视频长度
		setNotice("连接成功！");
	} else {
		setTimeout(waitForRemoteVideo, 100);
	}
}
window.onbeforeunload = function() {
	sendMessage({type : "bye"});
	if(peer) {
		peer.close();
	}
	socket.close();
}
if(!WebSocket) {
	alert("你的浏览器不支持WebSocket！");
} else if(!PeerConnection) {
	alert("你的浏览器不支持RTCPeerConnection！");
} else {
	setTimeout(initialize, 100); // 加载完成调用初始化方法
}
window.onbeforeunload = function() {
	socket.close();
}
*/