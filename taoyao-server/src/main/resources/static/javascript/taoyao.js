/**
 * 桃夭WebRTC终端示例
 */
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
	frameRate: 30,
	// 裁切
	// resizeMode: '',
	// 选摄像头：user|left|right|environment 
	facingMode: 'environment'
}
/** 兼容 */
const PeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
/** 桃夭 */
function Taoyao(
	webSocket,
	iceServer
) {
	this.webSocket = webSocket;
	this.iceServer = iceServer;
	this.audioStatus = true;
	this.videoStatus = true;
	this.audioStreamId = null;
	this.videoStreamId = null;
	this.audioConfig = defaultAudioConfig;
	this.videoConfig = defaultVideoConfig;
	/** 初始 */
	this.init = function() {
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
					this.audioConfig = false;
				}
				if(!videoDevice) {
					console.log('终端没有视频输入设备');
					this.videoConfig = false;
				}
			})
			.catch(e => console.log('获取终端设备失败', e));
		}
		return this;
	};
	/** 媒体 */
	this.buildUserMedia = function() {
		return new Promise((resolve, reject) => {
			console.log("获取终端媒体：", this.audioConfig, this.videoConfig);
			if(navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
				navigator.mediaDevices.getUserMedia({
					audio: this.audioConfig,
					video: this.videoConfig
				})
				.then(resolve)
				.catch(reject);
			} else if(navigator.getUserMedia) {
				navigator.getUserMedia({
					audio: this.audioConfig,
					video: this.videoConfig
				}, resolve, reject);
			} else {
				reject("获取终端媒体失败");
			}
		});
	};
	/** 本地 */
	this.local = async function(localVideoId, stream) {
		const localVideo = document.getElementById(localVideoId);
		if ('srcObject' in localVideo) {
			localVideo.srcObject = stream;
		} else {
			localVideo.src = URL.createObjectURL(stream);;
		}
		await localVideo.play();
	};
	/** 连接 */
	this.connect = function() {
	};
	/** 重连 */
	/** 定时 */
	/** 媒体 */
	/** 视频 */
	/** 心跳 */
}
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