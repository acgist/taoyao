package com.acgist.taoyao.signal.client.gb;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.acgist.taoyao.boot.config.GbProperties.Upper;
import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.service.GbURI;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.client.gb.GbDevice.Message;
import com.acgist.taoyao.signal.client.gb.GbXML.MessageType;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.client.ClientRegisterProtocol;

import gov.nist.core.LogLevels;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * GB-28181协议文档
 * https://openstd.samr.gov.cn/bzgk/std/newGbInfo?hcno=8BBC2475624A6C31DC34A28052B3923D
 */
@Slf4j
@RequiredArgsConstructor
public class GbServer implements SipListener {

    @Getter
    @Setter
    public static final class GbDeviceServer extends GbDevice {
        private String  realm;
        private String  nonce;
        private boolean connected = false;
    }

    @Getter
    @Setter
    public static final class GbDeviceClient extends GbDevice implements AutoCloseable {

        private String   name;
        private String   online;
        private String   status;
        private String   parental;
        private String   parentId;
        private GbClient gbClient;
        private final List<GbDeviceClient> channels = new ArrayList<>();

        public void push(com.acgist.taoyao.boot.model.Message message) {
            // TODO
        }

        @Override
        public void close() throws Exception {
            // TODO
        }

    }

    public static final record CallIdWrapper(String callId, String tag, Long seqNum) { }
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
    private ClientRegisterProtocol clientRegisterProtocol;

    private final Map<String, MessageWrapper> message = new ConcurrentHashMap<>();
    private final Map<String, GbDeviceServer> servers = new ConcurrentHashMap<>();
    private final Map<String, GbDeviceClient> clients = new ConcurrentHashMap<>();

    @Scheduled(cron = "0,30 * * * * ?")
    public void scheduled() {
        final LocalDateTime now = LocalDateTime.now();
        this.message.values().stream().forEach(v -> {
            if (Duration.between(v.time(), now).getSeconds() >= 30) {
                log.warn("消息超时：{}", v.callId());
                this.message.remove(v.callId);
            }
        });
        this.clients.values().stream().forEach(v -> {
            try {
                this.catalog(v);
            } catch (SipException | ParseException | InvalidArgumentException e) {
                log.warn("同步终端异常：{}", v.getDeviceId(), e);
            }
            if (Duration.between(v.getLastActiveTime(), now).getSeconds() >= this.gbProperties.getTimeout()) {
                log.warn("设备心跳超时：{}", v.getDeviceId());
                this.removeClient(v);
            }
        });
        this.servers.values().stream().filter(GbDeviceServer::isConnected).forEach(v -> {
            if (Duration.between(v.getLastActiveTime(), now).getSeconds() >= this.gbProperties.getTimeout()) {
                log.warn("服务心跳超时：{}", v.getDeviceId());
                v.setConnected(false);
            }
        });
        this.servers.values().stream().forEach(v -> {
            try {
                if (!v.isConnected()) {
                    this.register(v, this.gbProperties.getExpires(), 0L);
                } else if (Duration.between(v.getLastActiveTime(), now).getSeconds() >= 1800) {
                    this.register(v, this.gbProperties.getExpires(), 0L);
                } else {
                    this.keepalive(v);
                }
            } catch (Exception e) {
                log.error("定时任务异常：{}", v.getDeviceId(), e);
            }
        });
    }

    public void init() throws ObjectInUseException, PeerUnavailableException, InvalidArgumentException, TooManyListenersException , TransportNotSupportedException {
        final Properties properties = new Properties();
        // gov.nist.javax.sip.SipStackImpl
        properties.setProperty("javax.sip.STACK_NAME", this.gbProperties.getName());
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", String.valueOf(LogLevels.TRACE_TRACE));
        properties.setProperty("gov.nist.javax.sip.STACK_LOGGER", "com.acgist.taoyao.signal.client.gb.GbStackLogger");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOGGER", "com.acgist.taoyao.signal.client.gb.GbServerLogger");
        properties.setProperty("gov.nist.javax.sip.MAX_CONNECTIONS", "200");
        properties.setProperty("gov.nist.javax.sip.NIO_MAX_SOCKET_IDLE_TIME", "300000");
        // 配置SIP栈
        this.sipFactory     = SipFactory.getInstance();
        this.sipStack       = this.sipFactory.createSipStack(properties);
        this.headerFactory  = this.sipFactory.createHeaderFactory();
        this.addressFactory = this.sipFactory.createAddressFactory();
        this.messageFactory = this.sipFactory.createMessageFactory();
        // UDP
        log.info("监听UDP：{}:{}", this.gbProperties.getListen(), this.gbProperties.getPort());
        this.providerUdp = sipStack.createSipProvider(this.sipStack.createListeningPoint(this.gbProperties.getListen(), this.gbProperties.getPort(), "UDP"));
        this.providerUdp.addSipListener(this);
        // TCP
        log.info("监听TCP：{}:{}", this.gbProperties.getListen(), this.gbProperties.getPort());
        this.providerTcp = sipStack.createSipProvider(this.sipStack.createListeningPoint(this.gbProperties.getListen(), this.gbProperties.getPort(), "TCP"));
        this.providerTcp.addSipListener(this);
        // 输入日志
        log.info("SIP服务启动成功：{}:{}", this.gbProperties.getListen(), this.gbProperties.getPort());
    }

    public void registerServer() {
        final List<Upper> upper = this.gbProperties.getUpper();
        if(CollectionUtils.isEmpty(upper)) {
            return;
        }
        upper.forEach(v -> {
            final GbDeviceServer server = new GbDeviceServer();
            server.setTransport(v.transport());
            server.setHost(v.host());
            server.setPort(v.port());
            server.setDomainId(v.domainId());
            server.setDeviceId(v.deviceId());
            server.setUsername(v.username());
            server.setPassword(v.password());
            this.servers.put(v.deviceId(), server);
            try {
                this.register(server, this.gbProperties.getExpires(), 0L);
            } catch (SipException | ParseException | InvalidArgumentException e) {
                log.error("注册异常：{}", server.getDeviceId(), e);
            }
        });
    }

    @Override
    public void processRequest(RequestEvent event) {
        final Request request = event.getRequest();
        final String  method  = request.getMethod();
        final SipProvider  sipProvider = (SipProvider) event.getSource();
        final ToHeader     toHeader    = (ToHeader)    request.getHeader(ToHeader.NAME);
        final FromHeader   fromHeader  = (FromHeader)  request.getHeader(FromHeader.NAME);
        final URI to   = toHeader.getAddress().getURI();
        final URI from = fromHeader.getAddress().getURI();
        if (!Request.REGISTER.equals(method)) {
            final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(from);
            final GbDeviceServer gbDeviceServer = this.getGbDeviceServer(from);
            if (gbDeviceClient == null && gbDeviceServer == null) {
                log.warn("没有注册设备消息：{}", from);
                return;
            }
        }
        try {
            if (Request.REGISTER.equals(method)) {
                this.register(sipProvider, from, to, event, request);
            } else if (Request.SUBSCRIBE.equals(method)) {
                this.subscribe(sipProvider, from, to, event, request);
            } else if (Request.MESSAGE.equals(method)) {
                this.message(sipProvider, from, to, event, request);
            } else if (Request.INVITE.equals(method)) {
                this.invite(sipProvider, from, to, event, request);
            } else if (Request.NOTIFY.equals(method)) {
                this.notify(sipProvider, from, to, event, request);
            } else if (Request.ACK.equals(method)) {
                this.ack(sipProvider, from, to, event, request);
            } else if (Request.BYE.equals(method)) {
                this.bye(sipProvider, from, to, event, request);
            } else {
                log.info("没有适配SIP请求：{}", method);
            }
        } catch (Exception e) {
            log.error("处理SIP请求异常：{} - {} - {}", method, from, to, e);
        }
    }

    @Override
    public void processResponse(ResponseEvent event) {
        final Response response = event.getResponse();
        final SipProvider sipProvider = (SipProvider) event.getSource();
        final ToHeader    toHeader    = (ToHeader)    response.getHeader(ToHeader.NAME);
        final FromHeader  fromHeader  = (FromHeader)  response.getHeader(FromHeader.NAME);
        final CSeqHeader  cSeqHeader  = (CSeqHeader)  response.getHeader(CSeqHeader.NAME);
        final String method = cSeqHeader.getMethod();
        final URI    to     = toHeader.getAddress().getURI();
        final URI    from   = fromHeader.getAddress().getURI();
        try {
            if (Request.REGISTER.equals(method)) {
                this.register(sipProvider, from, to, event, response);
            } else if (Request.SUBSCRIBE.equals(method)) {
                this.subscribe(sipProvider, from, to, event, response);
            } else if (Request.MESSAGE.equals(method)) {
                this.message(sipProvider, from, to, event, response);
            } else if (Request.INVITE.equals(method)) {
                this.invite(sipProvider, from, to, event, response);
            } else if (Request.NOTIFY.equals(method)) {
                this.notify(sipProvider, from, to, event, response);
            } else if (Request.ACK.equals(method)) {
                this.ack(sipProvider, from, to, event, response);
            } else if (Request.BYE.equals(method)) {
                this.bye(sipProvider, from, to, event, response);
            } else {
                log.info("没有适配SIP响应：{}", method);
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
        log.info("对话终止：{} - {} - {}", dialog.getDialogId(), dialog.getLocalTag(), dialog.getRemoteTag());
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent event) {
        log.info("事务终止：{} - {} - {}", event.getSource(), event.getClientTransaction(), event.getServerTransaction());
    }

    private SipProvider getSipProvider(String transport) {
        return "UDP".equalsIgnoreCase(transport) ? this.providerUdp : this.providerTcp;
    }

    private SipURI createSipURI(String host, Integer port, String transport) throws ParseException {
        final SipURI sipURI = this.addressFactory.createSipURI(null, host);
        sipURI.setPort(port);
        sipURI.setTransportParam(transport);
        return sipURI;
    }

    private SipURI createSipURI(String deviceId, String domainId, String transport) throws ParseException {
        final SipURI sipURI = this.addressFactory.createSipURI(deviceId, domainId);
        sipURI.setTransportParam(transport);
        return sipURI;
    }

    private CallIdWrapper getCallId() {
        return new CallIdWrapper(UUID.randomUUID().toString(), UUID.randomUUID().toString(), System.currentTimeMillis() / 1000L);
    }

    private CallIdWrapper getCallId(Long seqNum) {
        return new CallIdWrapper(UUID.randomUUID().toString(), UUID.randomUUID().toString(), seqNum);
    }

    private GbURI getGbURI(URI uri) {
        final GbDeviceClient client = this.getGbDeviceClient(uri);
        if (client != null) {
            return client;
        }
        final GbDeviceServer server = this.getGbDeviceServer(uri);
        if (server != null) {
            return server;
        }
        return null;
    }

    private GbDeviceClient getGbDeviceClient(URI uri) {
        return this.clients.values().stream().filter(x -> x.equals(uri)).findFirst().orElse(null);
    }

    private GbDeviceServer getGbDeviceServer(URI uri) {
        return this.servers.values().stream().filter(x -> x.equals(uri)).findFirst().orElse(null);
    }

    private void registerClient(URI from, ViaHeader via) throws SipException, ParseException, InvalidArgumentException {
        final SipURI uri = (SipURI) from;
        final GbDeviceClient client = new GbDeviceClient();
        client.setHost(via.getHost());
        client.setPort(via.getPort());
        client.setTransport(via.getTransport());
        client.setDeviceId(uri.getUser());
        client.setDomainId(uri.getHost());
        log.info("注册设备信息：{}", client.deviceId);
        this.clients.put(client.deviceId, client);
        this.catalog(client);
        this.deviceInfo(client);
        this.deviceStatus(client);
        this.clientManager.open(new GbClient(this.gbProperties.getTimeout().longValue(), client));
        this.protocolManager.execute(this.clientRegisterProtocol.build(Map.of(
            Constant.CLIENT_ID,   client.deviceId,
            Constant.CLIENT_TYPE, ClientType.CAMERA.name(),
            Constant.USERNAME,    this.securityProperties.getUsername(),
            Constant.PASSWORD,    this.securityProperties.getPassword()
        )).toString(), client);
    }

    private void removeClient(GbDeviceClient client) {
        if (client != null) {
            log.info("删除设备信息：{}", client.deviceId);
            this.clients.remove(client.deviceId);
            this.clientManager.close(client);
        }
    }

    private String authorization(String username, String realm, String password, URI uri, String nonce) {
        final String prefix = DigestUtils.md5DigestAsHex((username + ":" + realm + ":" + password).getBytes());
        final String suffix = DigestUtils.md5DigestAsHex(("REGISTER:" + uri).getBytes());
        return DigestUtils.md5DigestAsHex((prefix + ":" + nonce + ":" + suffix).getBytes());
    }

    private RequestWrapper createRequest(String method, CallIdWrapper callId, GbURI fromURI, GbURI toURI) throws ParseException, InvalidArgumentException {
        final SipURI  requestUri = this.createSipURI(toURI.getHost(),       toURI.getPort(),       toURI.getTransport());
        final SipURI  fromUri    = this.createSipURI(fromURI.getDeviceId(), fromURI.getDomainId(), fromURI.getTransport());
        final SipURI  toUri      = this.createSipURI(toURI.getDeviceId(),   toURI.getDomainId(),   toURI.getTransport());
        final Request request    = this.messageFactory.createRequest(
            requestUri,
            method,
            this.headerFactory.createCallIdHeader(callId.callId),
            this.headerFactory.createCSeqHeader(callId.seqNum, method),
            this.headerFactory.createFromHeader(this.addressFactory.createAddress(fromUri), callId.tag),
            this.headerFactory.createToHeader(this.addressFactory.createAddress(toUri), null),
            List.of(this.headerFactory.createViaHeader(fromURI.getHost(), fromURI.getPort(), fromURI.getTransport(), null)),
            this.headerFactory.createMaxForwardsHeader(70)
        );
        return new RequestWrapper(request, requestUri, fromUri, toUri, callId);
    }

    private void register(GbDeviceServer upper, Integer expires, Long seqNum) throws SipException, ParseException, InvalidArgumentException {
        log.info("注册上级：{} - {}:{}", upper.getDeviceId(), upper.getHost(), upper.getPort());
        final SipProvider sipProvider = this.getSipProvider(upper.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.REGISTER, this.getCallId(seqNum), this.gbProperties, upper);
        final Request request = wrapper.request;
        final ContactHeader contact = this.headerFactory.createContactHeader(this.addressFactory.createAddress(wrapper.fromUri()));
        contact.setExpires(expires);
        request.addHeader(contact);
        request.addHeader(this.headerFactory.createExpiresHeader(expires));
        if(StringUtils.isNotEmpty(upper.getRealm()) && StringUtils.isNotEmpty(upper.getNonce())) {
            final String response = this.authorization(
                upper.getUsername(),
                upper.getRealm(),
                upper.getPassword(),
                wrapper.toUri(),
                upper.getNonce()
            );
            final AuthorizationHeader authorization = this.headerFactory.createAuthorizationHeader("Digest");
            authorization.setURI(wrapper.toUri());
            authorization.setRealm(upper.getRealm());
            authorization.setNonce(upper.getNonce());
            authorization.setResponse(response);
            authorization.setUsername(upper.getUsername());
            authorization.setAlgorithm("MD5");
            request.addHeader(authorization);
            // 清空信息
            upper.setRealm(null);
            upper.setNonce(null);
        }
        sipProvider.sendRequest(request);
    }

    private void keepalive(GbDeviceServer upper) throws SipException, ParseException, InvalidArgumentException {
        final SipProvider sipProvider = this.getSipProvider(upper.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, upper);
        final Request request = wrapper.request;
        request.setContent(GbXML.notify("Keepalive", upper.getDeviceId(), "OK"), this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        this.message.put(wrapper.callId.callId, new MessageWrapper(wrapper.callId.callId, "Keepalive", LocalDateTime.now()));
        sipProvider.sendRequest(request);
    }

    private void keepalive(GbDevice device) {
        if (device == null) {
            return;
        }
        log.debug("设备心跳：{}", device.deviceId);
        device.setLastActiveTime(LocalDateTime.now());
    }

    private void catalog(GbDeviceClient client) throws SipException, ParseException, InvalidArgumentException {
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, client);
        final Request request = wrapper.request;
        request.setContent(GbXML.query("Catalog", client.getDeviceId()), this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        sipProvider.sendRequest(request);
    }

    private void catalog(SipProvider sipProvider, Request request, GbURI fromURI, Message message) throws SipException, ParseException, InvalidArgumentException {
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, fromURI);
        // TODO
    }

    private void catalog(URI fromURI, Message message) {
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(fromURI);
        if (gbDeviceClient == null) {
            return;
        }
        // TODO
    }

    private void deviceInfo(GbDeviceClient client) throws SipException, ParseException, InvalidArgumentException {
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, client);
        final Request request = wrapper.request;
        request.setContent(GbXML.query("DeviceInfo", client.getDeviceId()), this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        sipProvider.sendRequest(request);
    }

    private void deviceInfo(SipProvider sipProvider, Request request, GbURI fromURI, Message message) throws SipException, ParseException, InvalidArgumentException {
        // TODO: 返回消息
    }

    private void deviceInfo(URI fromURI, Message message) {
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(fromURI);
        if (gbDeviceClient == null) {
            return;
        }
        gbDeviceClient.setName(message.getDeviceName());
    }

    private void deviceStatus(GbDeviceClient client) throws SipException, ParseException, InvalidArgumentException {
        final SipProvider sipProvider = this.getSipProvider(client.getTransport());
        final RequestWrapper wrapper = this.createRequest(Request.MESSAGE, this.getCallId(), this.gbProperties, client);
        final Request request = wrapper.request;
        request.setContent(GbXML.query("DeviceStatus", client.getDeviceId()), this.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        sipProvider.sendRequest(request);
    }

    private void deviceStatus(SipProvider sipProvider, Request request, GbURI fromURI, Message message) throws SipException, ParseException, InvalidArgumentException {
        // TODO: 返回消息
    }

    private void deviceStatus(URI fromURI, Message message) {
        final GbDeviceClient gbDeviceClient = this.getGbDeviceClient(fromURI);
        if (gbDeviceClient == null) {
            return;
        }
        gbDeviceClient.setOnline(message.getOnline());
        gbDeviceClient.setStatus(message.getStatus());
    }

    private void register(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        final ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
        final ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
        if (
            (contactHeader != null && contactHeader.getExpires() == 0) ||
            (expiresHeader != null && expiresHeader.getExpires() == 0)
        ) {
            log.info("设备取消注册：{}", from);
            this.removeClient(this.getGbDeviceClient(from));
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
            gbDeviceServer.setLastActiveTime(LocalDateTime.now());
        } else if (response.getStatusCode() == Response.UNAUTHORIZED) {
            final CSeqHeader cSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
            final WWWAuthenticateHeader wwwAuthenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
            gbDeviceServer.setRealm(wwwAuthenticateHeader.getRealm());
            gbDeviceServer.setNonce(wwwAuthenticateHeader.getNonce());
            log.info("服务注册确认：{}", to);
            this.register(gbDeviceServer, this.gbProperties.getExpires(), cSeqHeader.getSeqNumber() + 1);
        } else {
            log.warn("服务注册失败：{} - {}", response.getStatusCode(), to);
        }
    }

    private void subscribe(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
    }

    private void subscribe(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
    }

    private void message(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
        if(request.getRawContent() == null) {
            log.warn("处理消息无效：{}", from);
            return;
        }
        final String content = new String(request.getRawContent());
        final Message message = GbXML.message(content);
        final MessageType messageType = GbXML.messageType(content);
        if (message == null) {
            log.warn("消息解析失败：{}", content);
            return;
        }
        final GbURI fromURI = this.getGbURI(from);
        final Response response = this.messageFactory.createResponse(Response.OK, request);
        sipProvider.sendResponse(response);
        if ("Keepalive".equals(message.getCmdType())) {
            if (messageType == MessageType.NOTIFY) {
                this.keepalive(this.getGbDeviceClient(from));
            } else {
                // -
            }
        } else if("Catalog".equals(message.getCmdType())) {
            if (messageType == MessageType.QUERY) {
                this.catalog(sipProvider, request, fromURI, message);
            } else if (messageType == MessageType.RESPONSE) {
                this.catalog(from, message);
            } else {
                // -
            }
        } else if("DeviceInfo".equals(message.getCmdType())) {
            if (messageType == MessageType.QUERY) {
                this.deviceInfo(sipProvider, request, fromURI, message);
            } else if (messageType == MessageType.RESPONSE) {
                this.deviceInfo(from, message);
            } else {
                // -
            }
        } else if("DeviceStatus".equals(message.getCmdType())) {
            if (messageType == MessageType.QUERY) {
                this.deviceStatus(sipProvider, request, fromURI, message);
            } else if (messageType == MessageType.RESPONSE) {
                this.deviceStatus(from, message);
            } else {
                // -
            }
        } else {
            // -
        }
    }

    private void message(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
        final CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        final String callId = callIdHeader.getCallId();
        final MessageWrapper messageWrapper = this.message.get(callId);
        if (messageWrapper == null) {
            log.debug("没有注册消息回调：{}", callId);
            return;
        }
        if ("Keepalive".equals(messageWrapper.method)) {
            this.keepalive(this.getGbDeviceServer(to));
        } else {
            // -
        }
    }

    private void invite(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
    }

    private void invite(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
    }

    private void notify(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
    }

    private void notify(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
    }

    private void ack(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
    }

    private void ack(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
    }

    private void bye(SipProvider sipProvider, URI from, URI to, RequestEvent event, Request request) throws SipException, ParseException, InvalidArgumentException {
    }

    private void bye(SipProvider sipProvider, URI from, URI to, ResponseEvent event, Response response) throws SipException, ParseException, InvalidArgumentException {
    }
    
}
