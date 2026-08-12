package com.acgist.taoyao.signal.client.gb;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.SubjectHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.config.GbProperties;
import com.acgist.taoyao.boot.config.GbProperties.Server;
import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.service.GbURI;
import com.acgist.taoyao.boot.utils.DateUtils.DateTimeStyle;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.client.gb.GbDevice.AlarmstatusWrapper;
import com.acgist.taoyao.signal.client.gb.GbDevice.Device;
import com.acgist.taoyao.signal.client.gb.GbDevice.DeviceListWrapper;
import com.acgist.taoyao.signal.client.gb.GbDevice.Message;
import com.acgist.taoyao.signal.client.gb.GbXML.MessageType;
import com.acgist.taoyao.signal.protocol.Protocol;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.media.MediaTransportPlainCreateProtocol;
import com.acgist.taoyao.signal.protocol.room.RoomEnterProtocol;
import com.acgist.taoyao.signal.protocol.room.RoomExpelProtocol;
import com.acgist.taoyao.signal.protocol.room.RoomInviteProtocol;

import gov.nist.core.LogLevels;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * GB-28181协议文档
 * https://openstd.samr.gov.cn/bzgk/std/newGbInfo?hcno=8BBC2475624A6C31DC34A28052B3923D
 * 
 * 拉去终端
 * invite -> client -> invite response -> server -> ack -> client
 * bye    -> client -> bye response    -> server
 * 
 * TODO dialog 保活 re-invite 机制
 * TODO 上下级平台测试
 * 
 */
@Slf4j
@RequiredArgsConstructor
public class GbServer implements SipListener, IGbServer {

    @Getter
    @Setter
    public static final class GbDeviceServer extends GbDevice {
        private String  realm;
        private String  nonce;
        private boolean connected = false;
        private LocalDateTime registerTime;
    }

    @Getter
    @Setter
    public static final class GbDeviceClient extends GbDevice implements AutoCloseable {

        private String   name;         // 设备名称
        private String   online;       // 是否在线：ONLINE|OFFLINE
        private String   status;       // 是否在线：ON|OFF
        private String   manufacturer; // 设备厂商
        private String   model;        // 设备型号
        private String   civilCode;    // 行政区域
        private String   address;      // 安装地址
        private String   parental;     // 是否有子设备
        private String   parentId;     // 上级设备或者上级平台DeviceID
        private String   registerWay;  // 注册方式
        private String   secrecy;      // 是否涉密
        private String   firmware;     // 固件版本
        private Integer  channel;      // 通道数量
        private String   encode;       // 是否编码
        private String   record;       // 是否路线
        private GbClient gbClient;     // 代理设备
        private GbServer gbServer;     // 国标服务

        // 下级设备
        private final Map<String, GbDeviceClient> children = new ConcurrentHashMap<>();

        public boolean isChannel() {
            return "0".equals(this.parental);
        }

        public boolean isDevice() {
            return "1".equals(this.parental);
        }

        public boolean isOnline() {
            return "ON".equals(this.status);
        }

        public boolean isOffline() {
            return "OFF".equals(this.status);
        }

        public void push(com.acgist.taoyao.boot.model.Message message) {
            final String signal = message.getHeader().getSignal();
            switch (signal) {
                case RoomInviteProtocol.SIGNAL                -> this.invite(message);
                case RoomEnterProtocol.SIGNAL                 -> this.enter(message);
                case MediaTransportPlainCreateProtocol.SIGNAL -> this.plain(message);
                case RoomExpelProtocol.SIGNAL                 -> this.expel(message);
                default                                       -> log.warn("没有适配信令：{}", signal);
            }
        }

        private void invite(com.acgist.taoyao.boot.model.Message message) {
            final Map<String, Object> map = new HashMap<>(message.body());
            map.put(Constant.SUBSCRIBE_TYPE, "NONE");
            this.gbServer.protocolManager.execute(this.gbServer.roomEnterProtocol.build(map).toString(), this);
        }
        
        private void enter(com.acgist.taoyao.boot.model.Message message) {
            final Map<String, Object> map = message.body();
            final String roomId = map.get(Constant.ROOM_ID).toString();
            if (StringUtils.isEmpty(roomId)) {
                log.warn("进入房间失败：{}", message);
                return;
            }
            this.gbServer.protocolManager.execute(this.gbServer.mediaTransportPlainCreateProtocol.build(Map.of(
                "roomId" , roomId,
                "rtcpMux", false,
                "comedia", true
            )).toString(), this);
        }
        
        private void plain(com.acgist.taoyao.boot.model.Message message) {
            final Map<String, Object> map = message.body();
            final String host        = map.get(Constant.IP  ).toString();
            final String port        = map.get(Constant.PORT).toString();
            final String roomId      = map.get(Constant.ROOM_ID).toString();
            final String transportId = map.get(Constant.TRANSPORT_ID).toString();
            if (StringUtils.isEmpty(host) || StringUtils.isEmpty(port) || StringUtils.isEmpty(roomId) || StringUtils.isEmpty(transportId)) {
                log.warn("创建通道失败：{}", message);
                return;
            }
            try {
                final GbMedia gbMedia = this.gbServer.createMedia(roomId, transportId, this.deviceId, true);
                gbMedia.setAudioSsrc(100000L);
                gbMedia.setVideoSsrc(200000L);
                gbMedia.setServerHost(host);
                gbMedia.setServerPort(Integer.parseInt(port));
                gbMedia.setRoomId(roomId);
                gbMedia.setTransportId(transportId);
                this.gbServer.inviteToClient(this, gbMedia);
            } catch (Exception e) {
                log.error("邀请设备异常", e);
            }
        }

        private void expel(com.acgist.taoyao.boot.model.Message message) {
            this.gbServer.protocolManager.execute(this.gbServer.roomLeaveProtocol.build(message.body()).toString(), this);
            this.gbServer.closeMedia(this);
        }

        @Override
        public void close() throws Exception {
            this.gbServer.closeMedia(this);
        }

    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static final class GbMedia {

        private final String  callId;   // CallId
        private final String  localTag; // 本地TAG
        private final String  clientId; // 目标设备ID
        private final boolean local;    // 是否本地媒体
        // 会话信息
        private String   clientTag; // 目标设备TAG
        // 服务信息：如果上级邀请存在下面信息
        private String   serverId;  // 上级服务ID
        private String   serverTag; // 上级服务TAG=本地TAG
        private Request  request;   // 请求
        // 媒体信息
        private Integer  localPort;  // 本地服务端口
        private String   serverHost; // 目标主机
        private Integer  serverPort; // 目标地址
        private Long     srcSsrc;    // 原始媒体SSRC
        private Long     dstSsrc;    // 转发目标SSRC
        private Long     audioSsrc;  // 发送音频SSRC
        private Long     videoSsrc;  // 发送视频SSRC
        // 媒体状态
        private boolean  recv    = false; // 是否打开接受媒体
        private boolean  send    = false; // 是否发送媒体
        private boolean  forward = false; // 是否转发媒体
        // 本地信息
        private String   roomId;       // 通道ID
        private String   transportId;  // 通道ID

    }

    public static final record CallIdWrapper(String callId, String localTag, String remoteTag, Long seqNum) { }
    public static final record RequestWrapper(Request request, SipURI requestUri, SipURI fromUri, SipURI toUri, CallIdWrapper callId) { }
    public static final record MessageWrapper(String callId, String method, LocalDateTime time) { }

    private SipStack       sipStack;
    private SipFactory     sipFactory;
    private HeaderFactory  headerFactory;
    private AddressFactory addressFactory;
    private MessageFactory messageFactory;
    private SipProvider    providerUdp;
    private SipProvider    providerTcp;

    private final GbProperties       gbProperties;
    private final ClientManager      clientManager;
    private final ProtocolManager    protocolManager;
    private final SecurityProperties securityProperties;

    @Autowired
    private Protocol roomEnterProtocol;
    @Autowired
    private Protocol roomLeaveProtocol;
    @Autowired
    private Protocol mediaProduceProtocol;
    @Autowired
    private Protocol clientRegisterProtocol;
    @Autowired
    private Protocol clientHeartbeatProtocol;
    @Autowired
    private Protocol mediaTransportPlainCreateProtocol;

    private final Map<String, GbMedia>        media   = new ConcurrentHashMap<>();
    private final Map<String, MessageWrapper> message = new ConcurrentHashMap<>();
    private final Map<String, GbDeviceServer> servers = new ConcurrentHashMap<>();
    private final Map<String, GbDeviceClient> clients = new ConcurrentHashMap<>();

    @Scheduled(cron = "0,30 * * * * ?")
    public void scheduled() {
        final LocalDateTime now = LocalDateTime.now();
        this.message.values().stream().forEach(v -> {
            if (Duration.between(v.time(), now).getSeconds() >= 30) {
                log.warn("消息响应超时：{}", v.callId);
                this.message.remove(v.callId);
            }
        });
        this.clients.values().stream().forEach(v -> {
            try {
                this.catalogToClient(v);
            } catch (Exception e) {
                log.warn("查询设备异常：{}", v.getDeviceId(), e);
            }
            if (v.checkActiveTime(now, this.gbProperties.getTimeout())) {
                log.warn("设备心跳超时：{}", v.getDeviceId());
                this.setClientStatus(v, "OFF");
            }
        });
        this.servers.values().stream().forEach(v -> {
            try {
                if (!v.isConnected()) {
                    log.warn("服务没有连接：{}", v.getDeviceId());
                    this.registerToServer(v, this.gbProperties.getExpires());
                } else if (v.checkActiveTime(now, this.gbProperties.getTimeout())) {
                    log.warn("服务心跳超时：{}", v.getDeviceId());
                    v.setConnected(false);
                    this.registerToServer(v, this.gbProperties.getExpires());
                } else if (Duration.between(v.getRegisterTime(), now).getSeconds() >= 1800) {
                    // 重新注册
                    this.registerToServer(v, this.gbProperties.getExpires());
                } else {
                    // 心跳保活
                    this.keepaliveToServer(v);
                }
            } catch (Exception e) {
                log.error("服务心跳异常：{}", v.getDeviceId(), e);
            }
        });
    }

    @Override
    public void init() throws ObjectInUseException, PeerUnavailableException, InvalidArgumentException, TooManyListenersException , TransportNotSupportedException {
        // 配置媒体服务
        boolean loaded = false;
        final String jniLib = this.gbProperties.getJniLib();
        final String osName = System.getProperty("os.name").toLowerCase();
        if (jniLib.endsWith(".so") || jniLib.endsWith(".dll")) {
            loaded = GbMediaServer.loadJniLib(jniLib);
        } else if (osName.contains("win")) {
            loaded = GbMediaServer.loadJniLib(jniLib + ".dll");
        } else if (osName.contains("linux")) {
            loaded = GbMediaServer.loadJniLib("lib" + jniLib + ".so");
        } else {
            log.warn("不支持的系统：{}", osName);
        }
        if (loaded) {
            GbMediaServer.init();
            log.info("启动国标媒体服务成功");
        } else {
            log.warn("启动国标媒体服务失败");
        }
        // 配置SIP服务：gov.nist.javax.sip.SipStackImpl
        final Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", this.gbProperties.getName());
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", String.valueOf(LogLevels.TRACE_TRACE));
        properties.setProperty("gov.nist.javax.sip.STACK_LOGGER", "com.acgist.taoyao.signal.client.gb.GbStackLogger");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOGGER", "com.acgist.taoyao.signal.client.gb.GbServerLogger");
        properties.setProperty("gov.nist.javax.sip.MAX_CONNECTIONS", "200");
        properties.setProperty("gov.nist.javax.sip.NIO_MAX_SOCKET_IDLE_TIME", "300000");
        // 配置SIP工厂
        this.sipFactory     = SipFactory.getInstance();
        this.sipStack       = this.sipFactory.createSipStack(properties);
        this.headerFactory  = this.sipFactory.createHeaderFactory();
        this.addressFactory = this.sipFactory.createAddressFactory();
        this.messageFactory = this.sipFactory.createMessageFactory();
        // UDP
        log.info("监听SIP服务UDP：{}:{}", this.gbProperties.getListen(), this.gbProperties.getPort());
        this.providerUdp = sipStack.createSipProvider(this.sipStack.createListeningPoint(this.gbProperties.getListen(), this.gbProperties.getPort(), "UDP"));
        this.providerUdp.addSipListener(this);
        // TCP
        log.info("监听SIP服务TCP：{}:{}", this.gbProperties.getListen(), this.gbProperties.getPort());
        this.providerTcp = sipStack.createSipProvider(this.sipStack.createListeningPoint(this.gbProperties.getListen(), this.gbProperties.getPort(), "TCP"));
        this.providerTcp.addSipListener(this);
    }

    @Override
    public void registerServer() {
        final List<Server> server = this.gbProperties.getServer();
        if(CollectionUtils.isEmpty(server)) {
            return;
        }
        server.forEach(v -> {
            final GbDeviceServer gbDeviceServer = new GbDeviceServer();
            gbDeviceServer.setTransport(v.transport());
            gbDeviceServer.setHost(v.host());
            gbDeviceServer.setPort(v.port());
            gbDeviceServer.setDomainId(v.domainId());
            gbDeviceServer.setDeviceId(v.deviceId());
            gbDeviceServer.setUsername(v.username());
            gbDeviceServer.setPassword(v.password());
            this.servers.put(v.deviceId(), gbDeviceServer);
            try {
                this.registerToServer(gbDeviceServer, this.gbProperties.getExpires());
            } catch (SipException | ParseException | InvalidArgumentException e) {
                log.error("注册服务异常：{}", gbDeviceServer.getDeviceId(), e);
            }
        });
    }

    @Override
    public void processRequest(RequestEvent event) {
        final Request     request     = event.getRequest();
        final String      method      = request.getMethod();
        final SipProvider sipProvider = (SipProvider) event.getSource();
        final FromHeader  fromHeader  = (FromHeader)  request.getHeader(FromHeader.NAME);
        final ToHeader    toHeader    = (ToHeader)    request.getHeader(ToHeader.NAME);
        final URI         from        = fromHeader.getAddress().getURI();
        final URI         to          = toHeader.getAddress().getURI();
        if (!Request.REGISTER.equals(method)) {
            final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(from);
            final GbDeviceServer gbDeviceServer = this.getGbDeviceServer(from);
            if (gbDeviceClient == null && gbDeviceServer == null) {
                log.warn("消息来源没有注册：{}", from);
                return;
            }
        }
        try {
            switch (method) {
                case Request.REGISTER  -> this.register (sipProvider, from, to, event, request);
                case Request.SUBSCRIBE -> this.subscribe(sipProvider, from, to, event, request);
                case Request.MESSAGE   -> this.message  (sipProvider, from, to, event, request);
                case Request.INVITE    -> this.invite   (sipProvider, from, to, event, request);
                case Request.CANCEL    -> this.cancel   (sipProvider, from, to, event, request);
                case Request.NOTIFY    -> this.notify   (sipProvider, from, to, event, request);
                case Request.ACK       -> this.ack      (sipProvider, from, to, event, request);
                case Request.BYE       -> this.bye      (sipProvider, from, to, event, request);
                default                -> log.info("没有适配SIP请求：{}", method);
            }
        } catch (Exception e) {
            log.error("处理SIP请求异常：{} - {} - {}", method, from, to, e);
        }
    }

    @Override
    public void processResponse(ResponseEvent event) {
        final Response    response    = event.getResponse();
        final SipProvider sipProvider = (SipProvider) event.getSource();
        final CSeqHeader  cSeqHeader  = (CSeqHeader)  response.getHeader(CSeqHeader.NAME);
        final FromHeader  fromHeader  = (FromHeader)  response.getHeader(FromHeader.NAME);
        final ToHeader    toHeader    = (ToHeader)    response.getHeader(ToHeader.NAME);
        final String      method      = cSeqHeader.getMethod();
        final URI         from        = fromHeader.getAddress().getURI();
        final URI         to          = toHeader.getAddress().getURI();
        try {
            switch (method) {
                case Request.REGISTER  -> this.register (sipProvider, from, to, event, response);
                case Request.SUBSCRIBE -> this.subscribe(sipProvider, from, to, event, response);
                case Request.MESSAGE   -> this.message  (sipProvider, from, to, event, response);
                case Request.INVITE    -> this.invite   (sipProvider, from, to, event, response);
                case Request.CANCEL    -> this.cancel   (sipProvider, from, to, event, response);
                case Request.NOTIFY    -> this.notify   (sipProvider, from, to, event, response);
                case Request.ACK       -> this.ack      (sipProvider, from, to, event, response);
                case Request.BYE       -> this.bye      (sipProvider, from, to, event, response);
                default                -> log.info("没有适配SIP响应：{}", method);
            }
        } catch (Exception e) {
            log.error("处理SIP响应异常：{} - {} - {}", method, from, to, e);
        }
    }

    @Override
    public void processTimeout(TimeoutEvent event) {
        log.info("处理超时：{} - {}", event.getClientTransaction(), event.getServerTransaction());
    }

    @Override
    public void processIOException(IOExceptionEvent event) {
        log.info("处理异常：{}:{}", event.getHost(), event.getPort());
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent event) {
        final Dialog dialog = event.getDialog();
        if (dialog == null) {
            return;
        }
        log.info("对话终止：{} - {} - {}", dialog.getDialogId(), dialog.getLocalTag(), dialog.getRemoteTag());
        this.closeMedia(dialog.getCallId().getCallId());
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent event) {
        log.info("事务终止：{} - {} - {}", event.getSource(), event.getClientTransaction(), event.getServerTransaction());
    }

    private SipProvider getSipProvider(String transport) {
        return "UDP".equalsIgnoreCase(transport) ? this.providerUdp : this.providerTcp;
    }

    private SipURI createSipURI(String host, Integer port, String transport, String deviceId) throws ParseException {
        final SipURI sipURI = this.addressFactory.createSipURI(deviceId, host);
        sipURI.setPort(port);
        sipURI.setTransportParam(transport);
        return sipURI;
    }

    private SipURI createSipURI(String deviceId, String domainId, String transport) throws ParseException {
        final SipURI sipURI = this.addressFactory.createSipURI(deviceId, domainId);
        sipURI.setTransportParam(transport);
        return sipURI;
    }

    private GbMedia createMedia(String callId, String localTag, String clientId, boolean local) {
        // 严格意义来说需要判断id + localTag + remoteTag
        GbMedia gbMedia = this.media.get(callId);
        if (gbMedia != null) {
            return gbMedia;
        }
        return  new GbMedia(callId, localTag, clientId, local);
    }

    private void closeMedia(String callId) {
        final GbMedia old = this.media.remove(callId);;
        if (old == null) {
            return;
        }
        log.info("关闭媒体：{}", callId);
        GbMediaServer.close(callId);
    }

    private void closeMedia(GbDeviceClient client) {
        this.media.values().stream()
        .filter(x -> x.clientId.equals(client.deviceId))
        .map(GbMedia::getCallId)
        .forEach(x -> this.closeMedia(x));
    }

    private CallIdWrapper getCallId() {
        return new CallIdWrapper(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            null,
            System.currentTimeMillis() / 1000L
        );
    }

    private CallIdWrapper getCallId(String callId, String localTag, String remoteTag) {
        return new CallIdWrapper(
            callId,
            localTag,
            remoteTag,
            System.currentTimeMillis() / 1000L
        );
    }

    private GbDeviceClient getGbDeviceClient(URI request, URI uri, SubjectHeader subjectHeader) {
        final String deviceId;
        if (subjectHeader == null) {
            return this.getGbDeviceClient(request, uri);
        } else {
            final String subject = subjectHeader.getSubject();
            if (StringUtils.isEmpty(subject)) {
                return this.getGbDeviceClient(request, uri);
            }
            final int index = subject.indexOf(':');
            if (index < 0) {
                return this.getGbDeviceClient(request, uri);
            }
            deviceId = subject.substring(0, index).strip();
        }
        GbDeviceClient client = this.clients.get(deviceId);
        if (client != null) {
            return client;
        }
        return this.clients.values().stream()
        .map(x -> x.getChildren().get(deviceId))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
    }
    
    private GbDeviceClient getGbDeviceClient(URI uri) {
        return this.clients.values().stream()
        .filter(x -> x.equals(uri))
        .findFirst()
        .orElseGet(() -> {
            return this.clients.values().stream()
            .flatMap(x -> x.getChildren().values().stream())
            .filter(x -> x.equals(uri))
            .findFirst()
            .orElse(null);
        });
    }

    private GbDeviceClient getGbDeviceClient(URI request, URI uri) {
        return this.clients.values().stream()
        .filter(x -> x.equals(request) || x.equals(uri))
        .findFirst()
        .orElseGet(() -> {
            return this.clients.values().stream()
            .flatMap(x -> x.getChildren().values().stream())
            .filter(x -> x.equals(request) || x.equals(uri))
            .findFirst()
            .orElse(null);
        });
    }
    
    private GbDeviceServer getGbDeviceServer(URI uri) {
        return this.servers.values().stream().filter(x -> x.equals(uri)).findFirst().orElse(null);
    }

    private synchronized void registerClient(URI from, ViaHeader via) throws SipException, ParseException, InvalidArgumentException {
        final SipURI uri = (SipURI) from;
        GbDeviceClient client = this.clients.get(uri.getUser());
        if (client == null) {
            client = new GbDeviceClient();
        }
        client.setTransport(via.getTransport());
        client.setHost(via.getHost());
        client.setPort(via.getPort());
        client.setDomainId(uri.getHost());
        client.setDeviceId(uri.getUser());
        client.setParental("1");
        client.setParentId(this.gbProperties.getDeviceId());
        client.setGbServer(this);
        log.info("注册设备：{}", client.deviceId);
        this.clients.put(client.deviceId, client);
        // 查询设备信息状态
        this.deviceInfoToClient(client);
        this.deviceStatusToClient(client);
        // 查询设备下级列表
        this.catalogToClient(client);
    }

    private void setClientStatus(GbDeviceClient client, String status) {
        if (client == null) {
            return;
        }
        if ("ON".equals(status) || "ONLINE".equals(status)) {
            client.setStatus("ON");
            client.setOnline("ONLINE");
            // 上线代理
            this.registerClientChannelProxy(client);
        } else if ("OFF".equals(status) || "OFFLINE".equals(status)) {
            client.setStatus("OFF");
            client.setOnline("OFFLINE");
            // 修改下级设备状态
            if (!CollectionUtils.isEmpty(client.getChildren())) {
                client.getChildren().forEach((k, v) -> {
                    v.setStatus("OFF");
                    v.setOnline("OFFLINE");
                });
            }
            // 下线代理
            this.unregisterClientChannelProxy(client);
        } else {
            log.warn("设备状态错误：{} - {}", client.deviceId, status);
        }
    }

    private synchronized void registerClientChannelProxy(GbDeviceClient client) {
        if (client == null || client.isOffline()) {
            return;
        }
        if (client.isChannel()) {
            if (this.clientManager.getClients(client) != null) {
                return;
            }
            log.info("代理国标设备：{}", client.deviceId);
            this.clientManager.open(new GbClient(this.gbProperties.getTimeout().longValue(), client));
            this.protocolManager.execute(this.clientRegisterProtocol.build(Map.of(
                Constant.CLIENT_ID,   client.deviceId,
                Constant.CLIENT_TYPE, ClientType.CAMERA.name(),
                Constant.USERNAME,    this.securityProperties.getUsername(),
                Constant.PASSWORD,    this.securityProperties.getPassword()
            )).toString(), client);
        } else if (client.isDevice()) {
            if (CollectionUtils.isEmpty(client.getChildren())) {
                return;
            }
            client.getChildren().values().forEach(this::registerClientChannelProxy);
        } else {
            // -
        }
    }

    private synchronized void unregisterClientChannelProxy(GbDeviceClient client) {
        if (client == null || client.isOnline()) {
            return;
        }
        if (client.isChannel()) {
            if (this.clientManager.getClients(client) == null) {
                return;
            }
            log.info("取消国标设备代理：{}", client.deviceId);
            this.closeMedia(client);
            this.clientManager.close(client);
        } else if (client.isDevice()) {
            if (CollectionUtils.isEmpty(client.getChildren())) {
                return;
            }
            client.getChildren().values().forEach(this::unregisterClientChannelProxy);
        } else {
            // -
        }
    }

    private String authorization(String username, String realm, String password, URI uri, String nonce) {
        final String prefix = DigestUtils.md5DigestAsHex((username + ":" + realm + ":" + password).getBytes());
        final String suffix = DigestUtils.md5DigestAsHex(("REGISTER:" + uri).getBytes());
        return DigestUtils.md5DigestAsHex((prefix + ":" + nonce + ":" + suffix).getBytes());
    }

    private RequestWrapper createRequest(String method, CallIdWrapper callId, GbURI from, GbURI to) throws ParseException, InvalidArgumentException {
        final SipURI  requestUri = this.createSipURI(to  .getHost(),     to  .getPort(),     to  .getTransport(), to.getDeviceId());
        final SipURI  fromUri    = this.createSipURI(from.getDeviceId(), from.getDomainId(), from.getTransport());
        final SipURI  toUri      = this.createSipURI(to  .getDeviceId(), to  .getDomainId(), to  .getTransport());
        final Request request    = this.messageFactory.createRequest(
            requestUri,
            method,
            this.headerFactory.createCallIdHeader(callId.callId),
            this.headerFactory.createCSeqHeader(callId.seqNum, method),
            this.headerFactory.createFromHeader(this.addressFactory.createAddress(fromUri), callId.localTag),
            this.headerFactory.createToHeader(this.addressFactory.createAddress(toUri), callId.remoteTag),
            List.of(this.headerFactory.createViaHeader(from.getHost(), from.getPort(), from.getTransport(), UUID.randomUUID().toString())),
            this.headerFactory.createMaxForwardsHeader(70)
        );
        return new RequestWrapper(request, requestUri, fromUri, toUri, callId);
    }

    private void registerToServer(GbDeviceServer server, Integer expires) throws SipException, ParseException, InvalidArgumentException {
        log.info("注册上级：{} - {}:{}", server.getDeviceId(), server.getHost(), server.getPort());
        final SipProvider sipProvider = this.getSipProvider(server.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.REGISTER, this.getCallId(), this.gbProperties, server);
        final Request request = wrapper.request;
        final ContactHeader contact = this.headerFactory.createContactHeader(this.addressFactory.createAddress(wrapper.fromUri()));
        contact.setExpires(expires);
        request.addHeader(contact);
        request.addHeader(this.headerFactory.createExpiresHeader(expires));
        if(StringUtils.isNotEmpty(server.getRealm()) && StringUtils.isNotEmpty(server.getNonce())) {
            final String response = this.authorization(
                server.getUsername(),
                server.getRealm(),
                server.getPassword(),
                wrapper.toUri(),
                server.getNonce()
            );
            final AuthorizationHeader authorization = this.headerFactory.createAuthorizationHeader("Digest");
            authorization.setURI(wrapper.toUri());
            authorization.setRealm(server.getRealm());
            authorization.setNonce(server.getNonce());
            authorization.setResponse(response);
            authorization.setUsername(server.getUsername());
            authorization.setAlgorithm("MD5");
            request.addHeader(authorization);
            // 清空信息
            server.setRealm(null);
            server.setNonce(null);
        }
        sipProvider.sendRequest(request);
    }

    private void keepaliveToServer(GbDeviceServer server) throws SipException, ParseException, InvalidArgumentException {
        final SipProvider sipProvider = this.getSipProvider(server.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, server);
        final Request request = wrapper.request;
        request.setContent(
            GbXML.notify("Keepalive", server.getDeviceId(), "OK"),
            this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml")
        );
        this.message.put(wrapper.callId.callId, new MessageWrapper(wrapper.callId.callId, "Keepalive", LocalDateTime.now()));
        sipProvider.sendRequest(request);
    }

    private void keepaliveFromClient(GbDeviceClient client) {
        if (client == null) {
            return;
        }
        log.debug("设备心跳：{}", client.deviceId);
        client.setLastActiveTime(LocalDateTime.now());
        this.protocolManager.execute(this.clientHeartbeatProtocol.build(Map.of(
        )).toString(), client);
    }

    private void keepaliveFromServer(GbDeviceServer server) {
        if (server == null) {
            return;
        }
        log.debug("服务心跳：{}", server.deviceId);
        server.setLastActiveTime(LocalDateTime.now());
    }

    private void catalogToClient(GbDeviceClient client) throws SipException, ParseException, InvalidArgumentException {
        if (client == null) {
            return;
        }
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, client);
        final Request request = wrapper.request;
        request.setContent(
            GbXML.query("Catalog", client.getDeviceId()),
            this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml")
        );
        sipProvider.sendRequest(request);
    }

    private void catalogFromClient(GbDeviceClient client, Message message) {
        if (client == null) {
            return;
        }
        if (message.getDeviceList() == null || CollectionUtils.isEmpty(message.getDeviceList().getDeviceList())) {
            return;
        }
        message.getDeviceList().getDeviceList().forEach(v -> {
            final GbDeviceClient old = client.getChildren().get(v.getDeviceId());
            if (old != null) {
                this.setClientStatus(old, v.getStatus());
                return;
            }
            final GbDeviceClient gbDeviceClient = new GbDeviceClient();
            // 配置下级设备信息
            gbDeviceClient.setDeviceId(v.getDeviceId());
            gbDeviceClient.setName(v.getName());
            gbDeviceClient.setManufacturer(v.getManufacturer());
            gbDeviceClient.setModel(v.getModel());
            gbDeviceClient.setCivilCode(v.getCivilCode());
            gbDeviceClient.setAddress(v.getAddress());
            gbDeviceClient.setParental(v.getParental());
            gbDeviceClient.setParentId(v.getParentId());
            gbDeviceClient.setRegisterWay(v.getRegisterWay());
            gbDeviceClient.setSecrecy(v.getSecrecy());
            // 配置上级设备信息：域名ID设置上级设备ID
            gbDeviceClient.setTransport(client.getTransport());
            gbDeviceClient.setHost(client.getHost());
            gbDeviceClient.setPort(client.getPort());
            gbDeviceClient.setDomainId(client.getDeviceId());
            gbDeviceClient.setGbServer(this);
            client.getChildren().put(gbDeviceClient.getDeviceId(), gbDeviceClient);
            this.setClientStatus(gbDeviceClient, v.getStatus());
        });
    }

    private void catalogFromServer(SipProvider sipProvider, GbDeviceServer server, Message message) throws SipException, ParseException, InvalidArgumentException {
        if (server == null) {
            return;
        }
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, server);
        final Request request = wrapper.request;
        if (this.gbProperties.getDeviceId().equals(message.getDeviceId())) {
            final List<GbDeviceClient> list = new ArrayList<>();
            this.clients.values().forEach(v -> {
                list.add(v);
                if (CollectionUtils.isEmpty(v.getChildren())) {
                    return;
                }
                list.addAll(v.getChildren().values());
            });
            message.setSumNum(list.size());
            message.setDeviceList(new DeviceListWrapper(list.size(), list.stream().map(x -> {
                final Device device = new Device();
                device.setDeviceId(x.getDeviceId());
                device.setName(x.getName());
                device.setManufacturer(x.getManufacturer());
                device.setModel(x.getModel());
                device.setCivilCode(x.getCivilCode());
                device.setAddress(x.getAddress());
                device.setParental(x.getParental());
                device.setParentId(x.getParentId());
                device.setRegisterWay(x.getRegisterWay());
                device.setSecrecy(x.getSecrecy());
                device.setStatus(x.getStatus());
                return device;
            }).toList()));
        } else {
            log.warn("不能查询平台设备：{}", message.getDeviceId());
            return;
        }
        request.setContent(GbXML.write(message), this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        sipProvider.sendRequest(request);
    }

    private void deviceInfoToClient(GbDeviceClient client) throws SipException, ParseException, InvalidArgumentException {
        if (client == null) {
            return;
        }
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, client);
        final Request request = wrapper.request;
        request.setContent(
            GbXML.query("DeviceInfo", client.getDeviceId()),
            this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml")
        );
        sipProvider.sendRequest(request);
    }

    private void deviceInfoFromClient(GbDeviceClient client, Message message) {
        if (client == null) {
            return;
        }
        client.setName(message.getDeviceName());
        client.setManufacturer(message.getManufacturer());
        client.setModel(message.getModel());
        client.setFirmware(message.getFirmware());
        client.setChannel(message.getChannel());
    }

    private void deviceInfoFromServer(SipProvider sipProvider, GbDeviceServer server, Message message) throws SipException, ParseException, InvalidArgumentException {
        if (server == null) {
            return;
        }
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, server);
        final Request request = wrapper.request;
        if (this.gbProperties.getDeviceId().equals(message.getDeviceId())) {
            message.setResult("OK");
            message.setDeviceName(this.gbProperties.getName());
            message.setManufacturer("acgist");
            message.setModel("taoyao");
            message.setFirmware("1.0.0");
            message.setChannel(0);
        } else {
            log.warn("不能查询平台设备：{}", message.getDeviceId());
            return;
        }
        request.setContent(GbXML.write(message), this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        sipProvider.sendRequest(request);
    }

    private void deviceStatusToClient(GbDeviceClient client) throws SipException, ParseException, InvalidArgumentException {
        if (client == null) {
            return;
        }
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, client);
        final Request request = wrapper.request;
        request.setContent(
            GbXML.query("DeviceStatus", client.getDeviceId()),
            this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml")
        );
        sipProvider.sendRequest(request);
    }

    private void deviceStatusFromClient(GbDeviceClient client, Message message) {
        if (client == null) {
            return;
        }
        client.setEncode(message.getEncode());
        client.setRecord(message.getRecord());
        this.setClientStatus(client, message.getOnline());
    }

    private void deviceStatusFromServer(SipProvider sipProvider, GbDeviceServer server, Message message) throws SipException, ParseException, InvalidArgumentException {
        if (server == null) {
            return;
        }
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, server);
        final Request request = wrapper.request;
        if (this.gbProperties.getDeviceId().equals(message.getDeviceId())) {
            message.setResult("OK");
            message.setStatus("OK");
            message.setOnline("ONLINE");
            message.setEncode("OFF");
            message.setRecord("OFF");
            message.setDeviceTime(LocalDateTime.now().format(DateTimeStyle.YYYY_MM_DD_HH24_MM_SS_ISO.getDateTimeFormatter()));
            message.setAlarmstatus(new AlarmstatusWrapper(0, List.of()));
        } else {
            log.warn("不能查询平台设备：{}", message.getDeviceId());
            return;
        }
        request.setContent(GbXML.write(message), this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        sipProvider.sendRequest(request);
    }

    private synchronized GbMedia inviteToClient(GbDeviceClient client, GbMedia gbMedia) throws SipException, ParseException, InvalidArgumentException {
        if (client == null || gbMedia == null) {
            log.warn("邀请设备失败（无效信息）");
            return null;
        }
        if (!client.isChannel()) {
            log.warn("邀请设备失败（不是通道）：{}", client.deviceId);
            return null;
        }
        if (gbMedia.isRecv()) {
            log.info("邀请设备失败（已经接受）：{}", client.deviceId);
            return gbMedia;
        }
        final int port;
        if (gbMedia.getLocalPort() == null) {
            port = GbMediaServer.recv(gbMedia.callId, "UDP");
        } else {
            port = gbMedia.getLocalPort();
        }
        if (port <= 0) {
            log.warn("邀请设备失败（没有端口）：{} - {}", client.deviceId, port);
            return null;
        }
        gbMedia.setLocalPort(port);
        final CallIdWrapper callId = this.getCallId(gbMedia.getCallId(), gbMedia.getLocalTag(), gbMedia.getClientTag());
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.INVITE, callId, this.gbProperties, client);
        final Request request = wrapper.request;
        final SubjectHeader subjectHeader = this.headerFactory.createSubjectHeader(String.format("%s:0,%s:0", client.getDeviceId(), this.gbProperties.getDeviceId()));
        request.addHeader(subjectHeader);
        final String sdp = GbSDP.recvSDP(this.gbProperties.getDeviceId(), this.gbProperties.getHost(), port);
        log.debug("设备请求SDP:\n{}", sdp);
        request.setContent(sdp, this.headerFactory.createContentTypeHeader("Application", "SDP"));
        sipProvider.sendRequest(request);
        this.media.put(gbMedia.callId, gbMedia);
        if (gbMedia.isLocal()) {
            log.info("打开媒体：{} - {}", gbMedia.clientId, port);
        } else {
            log.info("打开媒体：{} -> {}={}:{} - {}", gbMedia.clientId, gbMedia.serverId, gbMedia.serverHost, gbMedia.serverPort, port);
        }
        return gbMedia;
    }

    private void cancelToClient(GbDeviceClient client, GbMedia gbMedia) throws SipException, ParseException, InvalidArgumentException {
        if (client == null || gbMedia == null) {
            return;
        }
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final CallIdWrapper callId = this.getCallId(gbMedia.getCallId(), gbMedia.getLocalTag(), gbMedia.getClientTag());
        final RequestWrapper wrapper = this.createRequest(Request.CANCEL, callId, this.gbProperties, client);
        sipProvider.sendRequest(wrapper.request);
    }

    private void ackToClient(GbDeviceClient client, GbMedia gbMedia) throws SipException, ParseException, InvalidArgumentException {
        if (client == null || gbMedia == null) {
            return;
        }
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final CallIdWrapper callId = this.getCallId(gbMedia.getCallId(), gbMedia.getLocalTag(), gbMedia.getClientTag());
        final RequestWrapper wrapper = this.createRequest(Request.ACK, callId, this.gbProperties, client);
        sipProvider.sendRequest(wrapper.request);
    }

    private void byeToClient(GbDeviceClient client, GbMedia gbMedia) throws SipException, ParseException, InvalidArgumentException {
        if (client == null || gbMedia == null) {
            return;
        }
        try {
            final SipProvider sipProvider = this.getSipProvider(client.getTransport());
            final CallIdWrapper callId = this.getCallId(gbMedia.getCallId(), gbMedia.getLocalTag(), gbMedia.getClientTag());
            final RequestWrapper wrapper = this.createRequest(Request.BYE, callId, this.gbProperties, client);
            sipProvider.sendRequest(wrapper.request);
        } finally {
            this.closeMedia(gbMedia.getCallId());
        }
    }

    private void register(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
        final ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
        if (
            (contactHeader != null && contactHeader.getExpires() == 0) ||
            (expiresHeader != null && expiresHeader.getExpires() == 0)
        ) {
            log.info("设备取消注册：{}", from);
            this.setClientStatus(this.getGbDeviceClient(from), "OFF");
            return;
        }
        final AuthorizationHeader authorizationHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (authorizationHeader == null) {
            log.info("设备注册应答：{}", from);
            final WWWAuthenticateHeader wwwAuthenticateHeader = this.headerFactory.createWWWAuthenticateHeader("Digest");
            wwwAuthenticateHeader.setAlgorithm("MD5");
            wwwAuthenticateHeader.setRealm(this.gbProperties.getDomainId());
            wwwAuthenticateHeader.setNonce(UUID.randomUUID().toString());
            final Response response = this.messageFactory.createResponse(Response.UNAUTHORIZED, request);
            response.addHeader(wwwAuthenticateHeader);
            sipProvider.sendResponse(response);
        } else {
            if (
                this.authorization(
                    authorizationHeader.getUsername(),
                    authorizationHeader.getRealm(),
                    this.gbProperties.getPassword(),
                    authorizationHeader.getURI(),
                    authorizationHeader.getNonce()
                ).equals(authorizationHeader.getResponse())
            ) {
                log.info("设备注册成功：{}", from);
                final Response response = this.messageFactory.createResponse(Response.OK, request);
                sipProvider.sendResponse(response);
                this.registerClient(from, (ViaHeader) request.getHeader(ViaHeader.NAME));
            } else {
                log.warn("设备注册失败：{}", from);
                final Response response = this.messageFactory.createResponse(Response.FORBIDDEN, request);
                sipProvider.sendResponse(response);
            }
        }
    }

    private void register(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        final GbDeviceServer gbDeviceServer = this.getGbDeviceServer(to);
        if (gbDeviceServer == null) {
            log.warn("服务注册无效：{}", to);
            return;
        }
        if (response.getStatusCode() == Response.OK) {
            log.info("服务注册成功：{}", to);
            gbDeviceServer.setConnected(true);
            gbDeviceServer.setRegisterTime(LocalDateTime.now());
            gbDeviceServer.setLastActiveTime(LocalDateTime.now());
        } else if (response.getStatusCode() == Response.UNAUTHORIZED) {
            final WWWAuthenticateHeader wwwAuthenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
            gbDeviceServer.setRealm(wwwAuthenticateHeader.getRealm());
            gbDeviceServer.setNonce(wwwAuthenticateHeader.getNonce());
            log.info("服务注册确认：{}", to);
            this.registerToServer(gbDeviceServer, this.gbProperties.getExpires());
        } else {
            log.warn("服务注册失败：{} - {}", response.getStatusCode(), to);
        }
    }

    private void subscribe(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final Response response = this.messageFactory.createResponse(Response.OK, request);
        sipProvider.sendResponse(response);
    }

    private void subscribe(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        // -
    }

    private void message(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final Response response = this.messageFactory.createResponse(Response.OK, request);
        sipProvider.sendResponse(response);
        if(request.getRawContent() == null) {
            log.warn("处理消息无效：{}", from);
            return;
        }
        final String content = new String(request.getRawContent());
        final Message message = GbXML.message(content);
        final MessageType messageType = GbXML.messageType(content);
        if (message == null || messageType == null) {
            log.warn("消息解析失败：{}", content);
            return;
        }
        final String cmdType = message.getCmdType();
        if ("Keepalive".equals(cmdType)) {
            if (messageType == MessageType.NOTIFY) {
                this.keepaliveFromClient(this.getGbDeviceClient(from));
            } else {
                log.warn("没有适配消息类型：{} - {}", messageType, cmdType);
            }
        } else if("Catalog".equals(cmdType)) {
            if (messageType == MessageType.QUERY) {
                this.catalogFromServer(sipProvider, this.getGbDeviceServer(from), message);
            } else if (messageType == MessageType.RESPONSE) {
                this.catalogFromClient(this.getGbDeviceClient(from), message);
            } else {
                log.warn("没有适配消息类型：{} - {}", messageType, cmdType);
            }
        } else if("DeviceInfo".equals(cmdType)) {
            if (messageType == MessageType.QUERY) {
                this.deviceInfoFromServer(sipProvider, this.getGbDeviceServer(from), message);
            } else if (messageType == MessageType.RESPONSE) {
                this.deviceInfoFromClient(this.getGbDeviceClient(from), message);
            } else {
                log.warn("没有适配消息类型：{} - {}", messageType, cmdType);
            }
        } else if("DeviceStatus".equals(cmdType)) {
            if (messageType == MessageType.QUERY) {
                this.deviceStatusFromServer(sipProvider, this.getGbDeviceServer(from), message);
            } else if (messageType == MessageType.RESPONSE) {
                this.deviceStatusFromClient(this.getGbDeviceClient(from), message);
            } else {
                log.warn("没有适配消息类型：{} - {}", messageType, cmdType);
            }
        } else {
            log.warn("没有适配消息类型：{} - {}", messageType, cmdType);
        }
    }

    private void message(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        final CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        final String callId = callIdHeader.getCallId();
        final MessageWrapper messageWrapper = this.message.remove(callId);
        if (messageWrapper == null) {
            return;
        }
        if ("Keepalive".equals(messageWrapper.method)) {
            this.keepaliveFromServer(this.getGbDeviceServer(to));
        } else {
            log.debug("没有适配消息回调：{}", callId);
        }
    }

    private synchronized void invite(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(request.getRequestURI(), to, (SubjectHeader) request.getHeader(SubjectHeader.NAME));
        final GbDeviceServer gbDeviceServer = this.getGbDeviceServer(from);
        if (gbDeviceClient == null || gbDeviceServer == null) {
            log.warn("邀请设备失败（无效媒体）：{} - {}", from, to);
            final Response response = this.messageFactory.createResponse(Response.SERVER_INTERNAL_ERROR, request);
            sipProvider.sendResponse(response);
            return;
        }
        if (request.getRawContent() == null) {
            log.warn("邀请设备失败（没有内容）：{} - {}", from, to);
            final Response response = this.messageFactory.createResponse(Response.SERVER_INTERNAL_ERROR, request);
            sipProvider.sendResponse(response);
            return;
        }
        final String content = new String(request.getRawContent());
        final String  host = GbSDP.getHost(content);
        final Integer port = GbSDP.getPort(content);
        if (host == null || port == null) {
            log.warn("邀请设备失败（内容错误）：{} - {} - {}", from, to, content);
            final Response response = this.messageFactory.createResponse(Response.SERVER_INTERNAL_ERROR, request);
            sipProvider.sendResponse(response);
            return;
        }
        final Dialog dialog = event.getDialog();
        final CallIdHeader callIdHeader = dialog.getCallId();
        // 直接使用上级CallId、RemoteTag
        GbMedia gbMedia = this.createMedia(callIdHeader.getCallId(), dialog.getRemoteTag(), gbDeviceClient.getDeviceId(), false);
        gbMedia.setServerId(gbDeviceServer.getDeviceId());
        gbMedia.setServerTag(dialog.getRemoteTag());
        gbMedia.setServerHost(host);
        gbMedia.setServerPort(port);
        gbMedia.setRequest(request);
        gbMedia = this.inviteToClient(gbDeviceClient, gbMedia);
        if (gbMedia == null) {
            final Response response = this.messageFactory.createResponse(Response.SERVER_INTERNAL_ERROR, request);
            sipProvider.sendResponse(response);
        } else {
            final Response response = this.messageFactory.createResponse(Response.TRYING, request);
            sipProvider.sendResponse(response);
        }
    }

    private synchronized void invite(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        final int statusCode = response.getStatusCode();
        if (statusCode == Response.TRYING) {
            return;
        }
        final Dialog dialog = event.getDialog();
        final CallIdHeader callIdHeader;
        if (dialog == null) {
            callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        } else {
            callIdHeader = dialog.getCallId();
        }
        final GbMedia gbMedia = this.media.get(callIdHeader.getCallId());
        if (gbMedia == null) {
            log.warn("邀请设备失败（无效媒体）：{}", callIdHeader.getCallId());
            return;
        }
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(to);
        if (gbDeviceClient == null) {
            log.warn("邀请设备失败（设备无效）：{}", callIdHeader.getCallId());
            this.closeMedia(callIdHeader.getCallId());
            if (!gbMedia.isLocal()) {
                final Response serverResponse = this.messageFactory.createResponse(statusCode, gbMedia.getRequest());
                sipProvider.sendResponse(serverResponse);
            }
        }
        if (statusCode != Response.OK) {
            log.warn("邀请设备失败（响应失败）：{} - {}", callIdHeader.getCallId(), statusCode);
            this.closeMedia(callIdHeader.getCallId());
            if (!gbMedia.isLocal()) {
                final Response serverResponse = this.messageFactory.createResponse(statusCode, gbMedia.getRequest());
                sipProvider.sendResponse(serverResponse);
            }
            return;
        }
        if(response.getRawContent() == null) {
            log.warn("邀请设备失败（没有响应）：{}", callIdHeader.getCallId());
            this.closeMedia(gbMedia.getCallId());
            if (!gbMedia.isLocal()) {
                final Response serverResponse = this.messageFactory.createResponse(Response.SERVER_INTERNAL_ERROR, gbMedia.getRequest());
                sipProvider.sendResponse(serverResponse);
            }
            return;
        }
        if (gbMedia.isRecv()) {
            log.info("邀请设备失败（已经接受）：{}", callIdHeader.getCallId());
            return;
        }
        gbMedia.setRecv(true);
        final String content = new String(response.getRawContent());
        log.debug("设备响应SDP:\n{}", content);
        final Long srcSsrc = GbSDP.getSsrc(content);
        gbMedia.setSrcSsrc(srcSsrc);
        gbMedia.setDstSsrc(srcSsrc);
        gbMedia.setClientTag(dialog.getRemoteTag());
        this.ackToClient(gbDeviceClient, gbMedia);
        // 处理媒体
        if (gbMedia.isLocal()) {
            if (gbMedia.isSend()) {
                log.info("媒体已经发送：{}", callIdHeader.getCallId());
                return;
            }
            log.info("发送媒体：{} - {}->{}:{}", gbMedia.getCallId(), gbMedia.getLocalPort(), gbMedia.getServerHost(), gbMedia.getServerPort());
            gbMedia.setSend(true);
            // 生成音频
            this.protocolManager.execute(this.mediaProduceProtocol.build(Map.of(
                "kind"         , "audio",
                "roomId"       , gbMedia.getRoomId(),
                "transportId"  , gbMedia.getTransportId(),
                "appData"      , Map.of(),
                "rtpParameters", Map.of(
                    "codecs"   , List.of(Map.of(
                        "mimeType"   , "audio/pcma",
                        "channels"   , 1,
                        "clockRate"  , 8000,
                        "payloadType", 8
                    )),
                    "encodings", List.of(Map.of(
                        "ssrc" , gbMedia.getAudioSsrc()
                    ))
                )
            )).toString(), gbDeviceClient);
            // 生成视频
            this.protocolManager.execute(this.mediaProduceProtocol.build(Map.of(
                "kind"         , "video",
                "roomId"       , gbMedia.getRoomId(),
                "transportId"  , gbMedia.getTransportId(),
                "appData"      , Map.of(),
                "rtpParameters", Map.of(
                    "codecs"   , List.of(Map.of(
                        "mimeType"    , "video/h264",
                        "clockRate"   , 90000,
                        "payloadType" , 107,
                        "parameters"  , Map.of(
                            "packetization-mode", 1,
                            "profile-level-id"  , "42e01f"
                        ),
                        "rtcpFeedback", List.of()
                    )),
                    "encodings", List.of(Map.of(
                        "ssrc" , gbMedia.getVideoSsrc()
                    ))
                )
            )).toString(), gbDeviceClient);
            // 开始发送媒体
            GbMediaServer.send(
                gbMedia.getCallId(),
                gbMedia.getServerHost(),
                gbMedia.getServerPort(),
                gbMedia.getSrcSsrc(),
                gbMedia.getAudioSsrc(),
                gbMedia.getVideoSsrc()
            );
        } else {
            final Response serverResponse = this.messageFactory.createResponse(Response.OK, gbMedia.getRequest());
            final String sdp = GbSDP.sendSDP(this.gbProperties.getDeviceId(), this.gbProperties.getHost(), gbMedia.getLocalPort());
            log.debug("服务响应SDP:\n{}", sdp);
            serverResponse.setContent(sdp, this.headerFactory.createContentTypeHeader("Application", "SDP"));
            sipProvider.sendResponse(serverResponse);
        }
    }

    private void cancel(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(request.getRequestURI(), to);
        final GbDeviceServer gbDeviceServer = this.getGbDeviceServer(from);
        if (gbDeviceClient == null || gbDeviceServer == null) {
            log.warn("邀请设备取消失败（无效设备）：{} - {}", from, to);
            return;
        }
        final Dialog dialog = event.getDialog();
        final CallIdHeader callIdHeader = dialog.getCallId();
        final GbMedia gbMedia = this.media.get(callIdHeader.getCallId());
        if (gbMedia == null) {
            log.warn("邀请设备取消失败（无效媒体）：{}", callIdHeader.getCallId());
            return;
        }
        this.cancelToClient(gbDeviceClient, gbMedia);
        final Response response = this.messageFactory.createResponse(Response.OK, request);
        sipProvider.sendResponse(response);
    }

    private void cancel(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        // -
    }

    private void notify(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        // -
    }

    private void notify(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        // -
    }

    private void ack(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(request.getRequestURI(), to);
        final GbDeviceServer gbDeviceServer = this.getGbDeviceServer(from);
        if (gbDeviceClient == null || gbDeviceServer == null) {
            log.warn("邀请设备确认失败（无效设备）：{} - {}", from, to);
            return;
        }
        final Dialog dialog = event.getDialog();
        final CallIdHeader callIdHeader = dialog.getCallId();
        final GbMedia gbMedia = this.media.get(callIdHeader.getCallId());
        if (gbMedia == null) {
            log.warn("邀请设备确认失败（无效媒体）：{}", callIdHeader.getCallId());
            return;
        }
        if (gbMedia.isForward()) {
            log.info("媒体已经转发：{}", callIdHeader.getCallId());
            return;
        }
        log.info("转发媒体：{} - {}->{}:{}", gbMedia.getCallId(), gbMedia.getLocalPort(), gbMedia.getServerHost(), gbMedia.getServerPort());
        gbMedia.setForward(true);
        GbMediaServer.forward(
            gbMedia.getCallId(),
            gbMedia.getServerHost(),
            gbMedia.getServerPort(),
            gbMedia.getSrcSsrc(),
            gbMedia.getDstSsrc()
        );
    }

    private void ack(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        // -
    }

    private void bye(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(request.getRequestURI(), to);
        final GbDeviceServer gbDeviceServer = this.getGbDeviceServer(from);
        if (gbDeviceClient == null || gbDeviceServer == null) {
            log.warn("邀请设备关闭失败（无效设备）：{} - {}", from, to);
            return;
        }
        final Dialog dialog = event.getDialog();
        final CallIdHeader callIdHeader = dialog.getCallId();
        final GbMedia gbMedia = this.media.get(callIdHeader.getCallId());
        if (gbMedia == null) {
            log.warn("邀请设备关闭失败（无效媒体）：{}", callIdHeader.getCallId());
            return;
        }
        this.byeToClient(gbDeviceClient, gbMedia);
        final Response response = this.messageFactory.createResponse(Response.OK, request);
        sipProvider.sendResponse(response);
    }

    private void bye(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        // -
    }
    
}
