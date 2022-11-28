/**
 * 桃夭WebRTC终端核心功能
 * 
 * 代码注意：
 * 1. undefined判断使用两个等号，其他情况使用三个。
*/
/** 兼容 */
const RTCIceCandidate = window.RTCIceCandidate || window.mozRTCIceCandidate || window.webkitRTCIceCandidate;
const RTCPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
const RTCSessionDescription = window.RTCSessionDescription || window.mozRTCSessionDescription || window.webkitRTCSessionDescription;
/** 默认音频配置 */
const defaultAudioConfig = {
	// 设备
	// deviceId : '',
	// 音量：0~1
	volume: 0.5,
	// 延迟大小（单位毫秒）：500毫秒以内较好
	latency: 0.4,
	// 采样数：16
	sampleSize: 16,
	// 采样率：8000|16000|32000|48000
	sampleRate: 32000,
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
/** 默认视频配置 */
const defaultVideoConfig = {
	// 设备
	// deviceId: '', 
	// 宽度
	width: 1280,
	// 高度
	height: 720,
	// 帧率
	frameRate: 24,
	// 选摄像头：user|left|right|environment 
	facingMode: 'environment'
}
/** 默认RTCPeerConnection配置 */
const defaultRPCConfig = {
	// ICE代理的服务器
	iceServers: null,
	// 传输通道绑定策略：balanced|max-compat|max-bundle
	bundlePolicy: 'balanced',
	// RTCP多路复用策略：require|negotiate
	rtcpMuxPolicy: 'require',
	// ICE传输策略：all|relay
	iceTransportPolicy: 'all'
	// ICE候选个数
	// iceCandidatePoolSize: 8
}
/** 信令配置 */
const signalConfig = {
	/** 当前终端SN */
	sn: localStorage.getItem('taoyao.sn') || 'taoyao',
	/** 当前版本 */
	version: '1.0.0',
	// 信令授权
	username: 'taoyao',
	password: 'taoyao'
};
/** 信令协议 */
const signalProtocol = {
	/** 直播信令 */
	live: {
	},
	/** 媒体信令 */
	media: {
		/** 发布 */
		publish: 5000,
		/** 订阅 */
		subscribe: 5002,
		/** 候选 */
		offer: 5997,
		/** Answer */
		answer: 5998,
		/** 候选 */
		candidate: 5999
	},
	/** 终端信令 */
	client: {
		/** 注册 */
		register:  2000,
		/** 下发配置 */
		config: 2004,
		/** 心跳 */
		heartbeat: 2005,
		/** 重启终端 */
		reboot: 2997,
	},
	/** 会议信令 */
	meeting: {
		/** 创建会议信令 */
		create: 4000,
		/** 进入会议信令 */
		enter: 4002,
	},
	/** 平台信令 */
	platform: {
		/** 异常 */
		error: 1999
	},
	/** 当前索引 */
	index: 100000,
	/** 最小索引 */
	minIndex: 100000,
	/** 最大索引 */
	maxIndex: 999999,
	/** 生成索引 */
	buildId: function() {
		if(this.index++ >= this.maxIndex) {
			this.index = this.minIndex;
		}
		return Date.now() + '' + this.index;
	},
	/** 生成信令消息 */
	buildProtocol: function(pid, body, id) {
		let message = {
			header: {
				v: signalConfig.version,
				id: id || this.buildId(),
				sn: signalConfig.sn,
				pid: pid,
			},
			'body': body
		};
		return message;
	}
};
/** 信令通道 */
const signalChannel = {
	/** 桃夭 */
	taoyao: null,
	/** 通道 */
	channel: null,
	/** 地址 */
	address: null,
	/** 回调 */
	callback: null,
	/** 回调事件 */
	callbackMapping: new Map(),
	/** 心跳时间 */
	heartbeatTime: 30 * 1000,
	/** 心跳定时器 */
	heartbeatTimer: null,
	/** 重连定时器 */
	reconnectTimer: null,
	/** 防止重复重连 */
	lockReconnect: false,
	/** 当前重连时间 */
	connectionTimeout: 5 * 1000,
	/** 最小重连时间 */
	minReconnectionDelay: 5 * 1000,
	/** 最大重连时间 */
	maxReconnectionDelay: 60 * 1000,
	/** 重连失败时间增长倍数 */
	reconnectionDelayGrowFactor: 2,
	/** 心跳 */
	heartbeat: function() {
		let self = this;
		if(self.heartbeatTimer) {
			clearTimeout(self.heartbeatTimer);
		}
		self.heartbeatTimer = setTimeout(function() {
			if (self.channel && self.channel.readyState === WebSocket.OPEN) {
				self.push(signalProtocol.buildProtocol(
					signalProtocol.client.heartbeat,
					{
						signal: 100,
						battery: 100
					}
				));
				self.heartbeat();
			} else {
				console.warn('发送心跳失败', self.channel);
			}
		}, self.heartbeatTime);
	},
	/** 连接 */
	connect: function(address, callback, reconnection = true) {
		let self = this;
		self.address = address;
		self.callback = callback;
		return new Promise((resolve, reject) => {
			console.debug('连接信令通道', address);
			self.channel = new WebSocket(address);
			self.channel.onopen = function(e) {
				console.debug('打开信令通道', e);
				// 注册终端
				self.push(signalProtocol.buildProtocol(
					signalProtocol.client.register,
					{
						ip: null,
						mac: null,
						signal: 100,
						battery: 100,
						username: signalConfig.username,
						password: signalConfig.password
					}
				));
				// 重置时间
				self.connectionTimeout = self.minReconnectionDelay
				// 开始心跳
				self.heartbeat();
				// 成功回调
				resolve(e);
			};
			self.channel.onclose = function(e) {
				console.error('信令通道关闭', self.channel, e);
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
			/**
			 * 回调策略：
			 * 1. 如果注册请求回调，同时执行结果返回true不再执行后面所有回调。
			 * 2. 如果注册全局回调，同时执行结果返回true不再执行后面所有回调。
			 * 3. 如果前面所有回调没有返回true执行默认回调。
			 */
			self.channel.onmessage = function(e) {
				console.debug('信令通道消息', e.data);
				let done = false;
				let data = JSON.parse(e.data);
				// 请求回调
				if(self.callbackMapping.has(data.header.id)) {
					try {
						done = self.callbackMapping.get(data.header.id)(data);
					} finally {
						self.callbackMapping.delete(data.header.id);
					}
				}
				// 全局回调
				if(self.callback) {
					done = self.callback(data);
				}
				// 默认回调
				if(!done) {
					self.defaultCallback(data);
				}
			};
		});
	},
	/** 重连 */
	reconnect: function() {
		let self = this;
		if (self.lockReconnect) {
			return;
		}
		self.lockReconnect = true;
		// 关闭旧的通道
		if(self.channel && self.channel.readyState === WebSocket.OPEN) {
			self.channel.close();
			self.channel = null;
		}
		if(self.reconnectTimer) {
			clearTimeout(self.reconnectTimer);
		}
		// 打开定时重连
		self.reconnectTimer = setTimeout(function() {
			console.info('信令通道重连', self.address, new Date());
			self.connect(self.address, self.callback, true);
			self.lockReconnect = false;
		}, self.connectionTimeout);
		if (self.connectionTimeout >= self.maxReconnectionDelay) {
			self.connectionTimeout = self.maxReconnectionDelay;
		} else {
			self.connectionTimeout = self.connectionTimeout * self.reconnectionDelayGrowFactor
		}
	},
	/** 发送消息 */
	push: function(data, callback) {
		// 注册回调
		if(data && callback) {
			this.callbackMapping.set(data.header.id, callback);
		}
		// 发送消息
		if(data && data.header) {
			this.channel.send(JSON.stringify(data));
		} else {
			this.channel.send(data);
		}
	},
	/** 关闭通道 */
	close: function() {
		clearTimeout(this.heartbeatTimer);
	},
	/** 默认回调 */
	defaultCallback: function(data) {
		console.debug('没有适配信令消息默认处理', data);
		switch(data.header.pid) {
		case signalProtocol.media.publish:
			this.defaultMediaPublish(data);
		break;
		case signalProtocol.media.subscribe:
			this.defaultMediaSubscribe(data);
		break;
		case signalProtocol.media.offer:
			this.defaultMediaOffer(data);
		break;
		case signalProtocol.media.answer:
			this.defaultMediaAnswer(data);
		break;
		case signalProtocol.media.candidate:
			this.defaultMediaCandidate(data);
		break;
		case signalProtocol.client.register:
		break;
		case signalProtocol.client.config:
			this.defaultClientConfig(data);
		break;
		case signalProtocol.client.heartbeat:
		break;
		case signalProtocol.client.reboot:
			this.defaultClientReboot(data);
		break;
		case signalProtocol.meeting.create:
		break;
		case signalProtocol.meeting.enter:
			this.defaultMeetingEnter(data);
		break;
		case signalProtocol.platform.error:
			console.error('信令发生错误', data);
		break;
		}
	},
	/** 终端默认回调 */
	defaultClientConfig: function(data) {
		this.taoyao
			.configMedia(data.body.media.audio, data.body.media.video)
			.configWebrtc(data.body.webrtc);
	},
	defaultClientReboot: function(data) {
		console.info('重启终端');
		location.reload();
	},
	/** 默认媒体回调 */
	defaultMediaPublish: function(data) {
	},
	defaultMediaSubscribe: function(data) {
		let self = this;
		const from = data.body.from;
		this.taoyao.remoteClientFilter(from, true);
		self.taoyao.localMediaChannel.createOffer().then(description => {
			console.debug('Local Create Offer', description);
			self.taoyao.localMediaChannel.setLocalDescription(description);
			self.push(signalProtocol.buildProtocol(
				signalProtocol.media.offer,
				{
					to: from,
					sdp: {
						sdp: description.sdp,
						type: description.type
					}
				}
			));
		});
	},
	defaultMediaOffer: function(data) {
		let self = this;
		const from = data.body.from;
		this.taoyao.remoteClientFilter(from, true);
		self.taoyao.remoteMediaChannel.setRemoteDescription(new RTCSessionDescription(data.body.sdp));
		self.taoyao.remoteMediaChannel.createAnswer().then(description => {
			console.debug('Remote Create Answer', description);
			self.taoyao.remoteMediaChannel.setLocalDescription(description);
			self.push(signalProtocol.buildProtocol(
				signalProtocol.media.answer,
				{
					to: from,
					sdp: {
						sdp: description.sdp,
						type: description.type
					}
				}
			));
		});
	},
	defaultMediaAnswer: function(data) {
		this.taoyao.localMediaChannel.setRemoteDescription(new RTCSessionDescription(data.body.sdp));
	},
	defaultMediaCandidate: function(data) {
		if(!this.taoyao.checkCandidate(data.body.candidate)) {
			console.debug('候选缺失要素', data);
			return;
		}
		console.debug('Set ICE Candidate', this.taoyao.remoteMediaChannel);
		if(data.body.type === 'local') {
			this.taoyao.remoteMediaChannel.addIceCandidate(new RTCIceCandidate(data.body.candidate));
		} else {
			this.taoyao.localMediaChannel.addIceCandidate(new RTCIceCandidate(data.body.candidate));
		}
	},
	/** 会议默认回调 */
	defaultMeetingEnter: function(data) {
		this.taoyao.mediaSubscribe(data.body.sn);
	}
};
/** 终端 */
function TaoyaoClient(
	sn,
	audioEnabled,
	videoEnabled
) {
	/** 终端标识 */
	this.sn = sn;
	/** 视频对象 */
	this.video = null;
	/** 媒体信息 */
	this.stream = null;
	this.audioTrack = [];
	this.videoTrack = [];
	/** 媒体状态：是否含有 */
	this.audioStatus = false;
	this.videoStatus = false;
	this.recordStatus = false;
	/** 媒体状态：是否播放 */
	this.audioEnabled = audioEnabled == undefined ? true : audioEnabled;
	this.videoEnabled = videoEnabled == undefined ? true : videoEnabled;
	/** 重置 */
	this.reset = function() {
	}
	/** 播放视频 */
	this.play = async function() {
		await this.video.play();
		return this;
	};
	/** 重新加载 */
	this.load = async function() {
		await this.video.load();
		return this;
	}
	/** 暂停视频 */
	this.pause = async function() {
		await this.video.pause();
		return this;
	};
	/** 关闭视频 */
	this.close = async function() {
		await this.video.close();
		return this;
	};
	/** 设置媒体 */
	this.buildStream = async function(videoId, stream, track) {
		if(!this.video && videoId) {
			this.video = document.getElementById(videoId);
		}
		if(!this.video) {
			throw new Error('视频对象无效：' + videoId);
		}
		if(!this.stream) {
			this.stream = new MediaStream();
			this.video.srcObject = this.stream;
		}
		if(track) {
			if(track.kind === 'audio') {
				this.buildAudioTrack(track);
			}
			if(track.kind === 'video') {
				this.buildVideoTrack(track);
			}
		} else {
			let audioTrack = stream.getAudioTracks();
			let videoTrack = stream.getVideoTracks();
			if(audioTrack && audioTrack.length) {
				audioTrack.forEach(v => this.buildAudioTrack(v));
			}
			if(videoTrack && videoTrack.length) {
				videoTrack.forEach(v => this.buildVideoTrack(v));
			}
		}
		console.debug('设置媒体', this.video, this.stream, this.audioTrack, this.videoTrack);
		await this.load();
		await this.play();
		return this;
	};
	/** 设置音频流 */
	this.buildAudioTrack = function(track) {
		// 关闭旧的
		// 创建新的
		this.audioStatus = true;
		this.audioTrack.push(track);
		if(this.audioEnabled) {
			this.stream.addTrack(track);
		}
	};
	/** 设置视频流 */
	this.buildVideoTrack = function(track) {
		// 关闭旧的
		// 创建新的
		this.videoStatus = true;
		this.videoTrack.push(track);
		if(this.videoEnabled) {
			this.stream.addTrack(track);
		}
	};
}
/** 桃夭 */
function Taoyao(
	webSocket,
	localClientAudioEnabled,
	localClientVideoEnabled
) {
	/** WebRTC配置 */
	this.webrtc = null;
	/** WebSocket地址 */
	// "wss://192.168.1.100:8888/websocket.signal"
	this.webSocket = webSocket || 'wss://' + location.host + '/websocket.signal';
	/** 设备状态 */
	this.audioEnabled = true;
	this.videoEnabled = true;
	/** 媒体配置 */
	this.audioConfig = defaultAudioConfig;
	this.videoConfig = defaultVideoConfig;
	/** 发送信令 */
	this.push = null;
	/** 本地终端 */
	this.localClient = null;
	this.localMediaChannel = null;
	/** 本地媒体状态 */
	this.localClientAudioEnabled = localClientAudioEnabled == undefined ? false : localClientAudioEnabled;
	this.localClientVideoEnabled = localClientVideoEnabled == undefined ? true : localClientVideoEnabled;
	/** 远程终端 */
	this.remoteClient = [];
	this.remoteMediaChannel = null;
	/** 信令通道 */
	this.signalChannel = null;
	/** 媒体配置 */
	this.configMedia = function(audio = {}, video = {}) {
		this.audioConfig = {...this.audioConfig, ...audio};
		this.videoConfig = {...this.videoConfig, ...video};
		console.debug('终端媒体配置', this.audioConfig, this.videoConfig);
		return this;
	};
	/** WebRTC配置 */
	this.configWebrtc = function(config = {}) {
		this.webrtc = config;
		this.webSocket = this.webrtc.signalAddress;
		defaultRPCConfig.iceServers = this.webrtc.stun.map(v => ({'urls': v}));
		console.debug('WebRTC配置', this.webrtc, defaultRPCConfig);
		return this;
	};
	/** 打开信令通道 */
	this.buildChannel = function(callback) {
		signalChannel.taoyao = this;
		this.signalChannel = signalChannel;
		// 不能直接this.push = this.signalChannel.push这样导致this对象错误
		this.push = function(data, callback) {
			this.signalChannel.push(data, callback)
		};
		return this.signalChannel.connect(this.webSocket, callback);
	};
	/** 打开本地媒体 */
	this.buildLocalMedia = function() {
		let self = this;
		return new Promise((resolve, reject) => {
			if(
				navigator.mediaDevices &&
				navigator.mediaDevices.getUserMedia &&
				navigator.mediaDevices.enumerateDevices
			) {
				navigator.mediaDevices.enumerateDevices()
				.then(list => {
					let audioEnabled = false;
					let videoEnabled = false;
					list.forEach(v => {
						console.debug('终端媒体设备', v, v.kind, v.label);
						switch(v.kind) {
						case 'audioinput':
							audioEnabled = true;
						break;
						case 'videoinput':
							videoEnabled = true;
						break;
						default:
							console.debug('没有适配设备', v.kind, v.label);
						break;
						}
					});
					if(!audioEnabled) {
						console.warn('终端没有音频输入设备');
						self.audioEnabled = false;
					}
					if(!videoEnabled) {
						console.warn('终端没有视频输入设备');
						self.videoEnabled = false;
					}
					console.debug('打开终端媒体', self.audioEnabled, self.videoEnabled, self.audioConfig, self.videoConfig);
					navigator.mediaDevices.getUserMedia({
						audio: self.audioEnabled ? self.audioConfig : false,
						video: self.videoEnabled ? self.videoConfig : false
					})
					.then(resolve)
					.catch(reject);
					// 兼容旧版
					// navigator.getUserMedia({
					// 	audio: self.audioConfig,
					// 	video: self.videoConfig
					// }, resolve, reject);
				})
				.catch(e => {
					console.error('检查终端设备异常', e);
					self.videoEnabled = false;
					self.videoEnabled = false;
				});
			} else {
				throw new Error('不支持的终端设备');
			}
		});
	};
	/** 远程终端过滤 */
	this.remoteClientFilter = function(sn, autoBuild) {
		let array = this.remoteClient.filter(v => v.sn === sn);
		let remote = null;
		if(array.length > 0) {
			remote = array[0];
		} else if(autoBuild) {
			remote = new TaoyaoClient(sn);
			this.remoteClient.push(remote);
		}
		return remote;
	};
	/** 关闭：关闭媒体 */
	this.close = function() {
		// TODO：释放资源
	};
	/** 关机：关闭媒体、关闭信令 */
	this.shutdown = function() {
		this.close();
	};
	/** 打开媒体通道 */
	this.buildMediaChannel = async function(localVideoId, stream) {
		let self = this;
		// 本地视频
		this.localClient = new TaoyaoClient(signalConfig.sn, this.localClientAudioEnabled, this.localClientVideoEnabled);
		await this.localClient.buildStream(localVideoId, stream);
		// 本地通道
		this.localMediaChannel = new RTCPeerConnection(defaultRPCConfig);
		this.localClient.audioTrack.forEach(v => this.localMediaChannel.addTrack(v, this.localClient.stream));
		this.localClient.videoTrack.forEach(v => this.localMediaChannel.addTrack(v, this.localClient.stream));
		this.localMediaChannel.ontrack = function(e) {
			console.debug('Local Media Track', e);
		};
		this.localMediaChannel.ondatachannel = function(channel) {
			channel.onopen = function() {
				console.debug('Local DataChannel Open');
			}
			channel.onmessage = function(data) {
				console.debug('Local DataChannel Message', data);
			}
			channel.onclose = function() {
				console.debug('Local DataChannel Close');
			}
			channel.onerror = function(e) {
				console.debug('Local DataChannel Error', e);
			}
		};
		this.localMediaChannel.onicecandidate = function(e) {
			// TODO：判断给谁
			let to = self.remoteClient.map(v => v.sn)[0];
			if(!self.checkCandidate(e.candidate)) {
				console.debug('Send Local ICE Candidate Fail', e);
				return;
			}
			console.debug('Send Local ICE Candidate', to, e);
			self.push(signalProtocol.buildProtocol(
				signalProtocol.media.candidate,
				{
					to: to,
					type: 'local',
					candidate: e.candidate
				}
			));
		};
		// 远程通道
		this.remoteMediaChannel = new RTCPeerConnection(defaultRPCConfig);
		this.remoteMediaChannel.ontrack = function(e) {
			console.debug('Remote Media Track', e);
			// TODO：匹配
			let remote = self.remoteClient[0];
			remote.buildStream(remote.sn, e.streams[0], e.track);
		};
		this.remoteMediaChannel.ondatachannel = function(channel) {
			channel.onopen = function() {
				console.debug('Remote DataChannel Open');
			}
			channel.onmessage = function(data) {
				console.debug('Remote DataChannel Message', data);
			}
			channel.onclose = function() {
				console.debug('Remote DataChannel Close');
			}
			channel.onerror = function(e) {
				console.debug('Remote DataChannel Error', e);
			}
		};
		this.remoteMediaChannel.onicecandidate = function(e) {
			// TODO：判断给谁
			let to = self.remoteClient.map(v => v.sn)[0];
			if(!self.checkCandidate(e.candidate)) {
				console.debug('Send Remote ICE Candidate Fail', e);
				return;
			}
			console.debug('Send Remote ICE Candidate', to, e);
			self.push(signalProtocol.buildProtocol(
				signalProtocol.media.candidate,
				{
					to: to,
					type: 'remote',
					candidate: e.candidate
				}
			));
		};
		console.debug('打开媒体通道', this.localMediaChannel, this.remoteMediaChannel);
		return this;
	};
	/** 校验candidate */
	this.checkCandidate = function(candidate) {
		if(
			!candidate ||
			!candidate.candidate ||
			candidate.sdpMid === null ||
			candidate.sdpMid === null ||
			candidate.sdpMLineIndex === undefined ||
			candidate.sdpMLineIndex === undefined
		) {
			return false;
		}
		return true;
	};
	/** 媒体信令 */
	this.mediaSubscribe = function(sn, callback) {
		let self = this;
		self.remoteClientFilter(sn, true);
		self.push(signalProtocol.buildProtocol(
			signalProtocol.media.subscribe,
			{
				to: sn
			}
		), callback);
	};
	/** 会议信令 */
	this.meetingCreate = function(callback) {
		let self = this;
		self.push(signalProtocol.buildProtocol(
			signalProtocol.meeting.create
		), callback);
	}
	this.meetingEnter = function(id, callback) {
		let self = this;
		self.push(signalProtocol.buildProtocol(
			signalProtocol.meeting.enter,
			{
				id: id
			}
		), callback);
	};
};
