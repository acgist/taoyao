#include <map>
#include <mutex>
#include <atomic>
#include <string>
#include <thread>
#include <cstring>
#include <functional>

#include <jni.h>

#include "mpeg-ps.h"
#include "rtp-header.h"
#include "rtp-demuxer.h"
#include "rtp-payload.h"
#include "rtcp-header.h"

#ifdef _WIN32
#pragma comment(lib, "ws2_32.lib")
#include <winsock2.h>
#include <ws2tcpip.h>
typedef SOCKET socket_t;
#define SOCKET_INVALID INVALID_SOCKET
static inline int socket_close(socket_t socket) {
    return ::closesocket(socket);
}
#else
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>
typedef int socket_t;
#define SOCKET_INVALID (-1)
static inline int socket_close(socket_t socket) {
    return ::close(socket);
}
#endif

const static int rtp_pkt_size  =   64 * 1024;
const static int rtcp_pkt_size =   32 * 1024;
const static int enc_buf_size  = 1024 * 1024 + 4;

class GbMediaServer;

static std::mutex                            gb_media_server_map_mtx;
static std::map<std::string, GbMediaServer*> gb_media_server_map;

static void  rtp_encode_free   (void* param, void* packet);
static void* rtp_encode_alloc  (void* param, int   length);
static int   rtp_encode_packet (void* param, const void* packet, int length, uint32_t timestamp, int flags);
static int   rtp_demuxer_packet(void* param, const void* packet, int length, uint32_t timestamp, int flags);
static int   ps_demuxer_packet (void* param, int stream, int codecid, int flags, int64_t pts, int64_t dts, const void* data, size_t length);

class GbMediaServer {

friend int rtp_encode_packet(void* param, const void* packet, int length, uint32_t timestamp, int flags);

public:
    char         *    rtp_buffer;
    char         *    rtcp_buffer;
    char         *    encode_buffer;
    void         *    audio_encoder;
    void         *    video_encoder;
    rtp_demuxer_t*    rtp_demuxer;
    rtp_payload_t     rtp_handler;
    ps_demuxer_t *    ps_demuxer;
    uint16_t          local_port; // 本地端口（本地服务端）
    uint32_t          src_ssrc;   // 原始ssrc
    uint32_t          dst_ssrc;   // 目标ssrc
    uint32_t          audio_ssrc; // 音频ssrc
    uint32_t          video_ssrc; // 视频ssrc
    socket_t          client;     // 客户端socket
    socket_t          server;     // 服务端socket
    sockaddr_in       rtp_addr;   // RTP 发送地址（发送客户端）
    sockaddr_in       rtcp_addr;  // RTCP发送地址（回给服务的）
    std::mutex        mutex;      // 服务端互斥锁
    std::thread       thread;     // 接收线程
    std::atomic<bool> running;    // 是否运行
    std::function<void(const char* data, int length)> on_recv; // 接收回调

public:
    GbMediaServer();
    ~GbMediaServer();

private:
    bool init_client(const char* host, uint16_t port);
    bool init_server(                  uint16_t port);
    void send       (const sockaddr_in& addr, const char* data, int length);
public:
    void recv   (uint16_t port);
    void send   (const char* host, uint16_t port, uint32_t src_ssrc, uint32_t audio_ssrc, uint32_t video_ssrc);
    void forward(const char* host, uint16_t port, uint32_t src_ssrc, uint32_t dst_ssrc);

};

GbMediaServer::GbMediaServer() {
    this->rtp_buffer    = new char[rtp_pkt_size];
    this->rtcp_buffer   = new char[rtcp_pkt_size];
    this->encode_buffer = new char[enc_buf_size] { 0, 0, 0, 1, };
    this->running       = false;
    this->audio_encoder = nullptr;
    this->video_encoder = nullptr;
    this->rtp_demuxer   = nullptr;
    this->ps_demuxer    = nullptr;
    this->local_port    = 0;
    this->client        = SOCKET_INVALID;  
    this->server        = SOCKET_INVALID;
}

GbMediaServer::~GbMediaServer() {
    if (this->client != SOCKET_INVALID) {
        socket_close(this->client);
        this->client = SOCKET_INVALID;
    }
    if (this->server != SOCKET_INVALID) {
        socket_close(this->server);
        this->server = SOCKET_INVALID;
    }
    if (this->audio_encoder) {
        rtp_payload_encode_destroy(this->audio_encoder);
        this->audio_encoder = nullptr;
    }
    if (this->video_encoder) {
        rtp_payload_encode_destroy(this->video_encoder);
        this->video_encoder = nullptr;
    }
    if (this->rtp_demuxer) {
        rtp_demuxer_destroy(&this->rtp_demuxer);
        this->rtp_demuxer = nullptr;
    }
    if (this->ps_demuxer) {
        ps_demuxer_destroy(this->ps_demuxer);
        this->ps_demuxer = nullptr;
    }
    delete[] this->rtp_buffer;
    delete[] this->rtcp_buffer;
    delete[] this->encode_buffer;
}

bool GbMediaServer::init_client(const char* host, uint16_t port) {
    this->client = ::socket(AF_INET, SOCK_DGRAM, 0);
    if (this->client == SOCKET_INVALID) {
        return false;
    }
    int opt_reuseaddr = 1;
    int opt_rcvbuf    = 1024 * 1024;
    ::setsockopt(this->client, SOL_SOCKET, SO_REUSEADDR, (char*) &opt_reuseaddr, sizeof(opt_reuseaddr));
    ::setsockopt(this->client, SOL_SOCKET, SO_RCVBUF,    (char*) &opt_rcvbuf,    sizeof(opt_rcvbuf));
    this->rtp_addr.sin_family      = AF_INET;
//  this->rtp_addr.sin_addr.s_addr = ::inet_addr(host);
    this->rtp_addr.sin_port        = ::htons(port);
    if (::inet_pton(AF_INET, host, &this->rtp_addr.sin_addr) != 1) {
        return false;
    }
    return true;
}

bool GbMediaServer::init_server(uint16_t port) {
    this->server = ::socket(AF_INET, SOCK_DGRAM, 0);
    if (this->server == SOCKET_INVALID) {
        return false;
    }
    // 配置阻塞模式
    #ifdef _WIN32
    u_long mode = 1;
    ::ioctlsocket(this->server, FIONBIO, &mode);
    #else
    int flags = ::fcntl(this->server, F_GETFL, 0);
    ::fcntl(this->server, F_SETFL, flags | O_NONBLOCK);
    #endif
    int opt_reuseaddr = 1;
    int opt_rcvbuf    = 1024 * 1024;
    ::setsockopt(this->server, SOL_SOCKET, SO_REUSEADDR, (char*) &opt_reuseaddr, sizeof(opt_reuseaddr));
    ::setsockopt(this->server, SOL_SOCKET, SO_RCVBUF,    (char*) &opt_rcvbuf,    sizeof(opt_rcvbuf));
    // 配置绑定端口
    sockaddr_in sin{};
    #ifdef _WIN32
    int sin_len = sizeof(sin);
    #else
    socklen_t sin_len = sizeof(sin);
    #endif
    sin.sin_family      = AF_INET;
    sin.sin_addr.s_addr = INADDR_ANY;
    sin.sin_port        = ::htons(port);
    if(::bind(this->server, (const sockaddr*) &sin, sin_len) != 0) {
        return false;
    }
    if(::getsockname(this->server, (sockaddr*) &sin, &sin_len) != 0) {
        return false;
    }
    this->local_port = ::ntohs(sin.sin_port);
    return true;
}

void GbMediaServer::recv(uint16_t port) {
    std::lock_guard<std::mutex> lock(this->mutex);
    if (this->running) {
        return;
    }
    this->running  = true;
    this->init_server(port);
    this->thread = std::thread([this]() {
        int count = 0;
        sockaddr_in sin{};
        while (this->running) {
            fd_set rfds;
            FD_ZERO(&rfds);
            FD_SET(this->server, &rfds);
            timeval tv;
            tv.tv_sec  = 1;
            tv.tv_usec = 0;
            #ifdef _WIN32
            int nfds = 0;
            #else
            int nfds = this->server + 1;
            #endif
            int select_ret = ::select(nfds, &rfds, nullptr, nullptr, &tv);
            if (select_ret < 0) {
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
                continue;
            }
            if (select_ret == 0) {
                ++count;
                if (count % 10 == 0) {
                    // TODO 心跳
                }
                if (count >= 600) {
                    this->running = false;
                }
                continue;
            }
            if (!FD_ISSET(this->server, &rfds)) {
                continue;
            }
            count = 0;
            while (true) {
                #ifdef _WIN32
                int sin_len = sizeof(sin);
                int recv_ret = ::recvfrom(this->server, this->rtp_buffer, rtp_pkt_size, 0, (sockaddr*) &sin, &sin_len);
                if (recv_ret == SOCKET_ERROR) {
                    int err = WSAGetLastError();
                    if (err == WSAEWOULDBLOCK) {
                        break;
                    }
                    std::printf("recvfrom error: %d\n", err);
                    break;
                }
                #else
                socklen_t sin_len = sizeof(sin);
                ssize_t recv_ret = ::recvfrom(this->server, this->rtp_buffer, rtp_pkt_size, 0, (sockaddr*) &sin, &sin_len);
                if (recv_ret < 0) {
                    if (errno == EAGAIN || errno == EWOULDBLOCK) {
                        break;
                    }
                    std::printf("recvfrom error: %d\n", errno);
                    break;
                }
                #endif
                if (recv_ret == 0) {
                    continue;
                }
                if(recv_ret < 12) {
                    continue;
                }
                uint8_t v = (this->rtp_buffer[0] >> 6) & 0x03;
                if(v != RTP_VERSION) {
                    continue;
                }
                if (this->on_recv) {
                    this->on_recv(this->rtp_buffer, recv_ret);
                }
            }
        }
    });
}

void GbMediaServer::send(const char* host, uint16_t port, uint32_t src_ssrc, uint32_t audio_ssrc, uint32_t video_ssrc) {
    std::lock_guard<std::mutex> lock(this->mutex);
    if (this->on_recv || !this->running) {
        return;
    }
    if (this->audio_encoder || this->video_encoder || this->rtp_demuxer) {
        return;
    }
    this->init_client(host, port);
    this->src_ssrc           = src_ssrc;
    this->audio_ssrc         = audio_ssrc;
    this->video_ssrc         = video_ssrc;
    this->rtp_handler.free   = rtp_encode_free;
	this->rtp_handler.alloc  = rtp_encode_alloc;
	this->rtp_handler.packet = rtp_encode_packet;
    this->rtp_demuxer        = rtp_demuxer_create(100, 90000, 96, "PS", rtp_demuxer_packet, this);
    this->audio_encoder      = rtp_payload_encode_create(  8, "PCMA", (uint16_t) audio_ssrc, audio_ssrc, &this->rtp_handler, this);
    this->video_encoder      = rtp_payload_encode_create(107, "H264", (uint16_t) video_ssrc, video_ssrc, &this->rtp_handler, this);
    this->ps_demuxer         = ps_demuxer_create(ps_demuxer_packet, this);
    this->on_recv = [this](const char* data, int length) {
        // 处理RTP数据
        int ret = rtp_demuxer_input(this->rtp_demuxer, data, length);
        if (ret <= 0) {
            return;
        }
        // 处理RTCP数据
//      ret = rtp_demuxer_rtcp(this->rtp_demuxer, this->rtcp_buffer, rtcp_pkt_size);
//      if (ret > 0) {
//          this->send(this->rtcp_addr, this->rtcp_buffer, ret);
//      }
    };
}

void GbMediaServer::forward(const char* host, uint16_t port, uint32_t src_ssrc, uint32_t dst_ssrc) {
    std::lock_guard<std::mutex> lock(this->mutex);
    if (this->on_recv || !this->running) {
        return;
    }
    this->init_client(host, port);
    this->src_ssrc = src_ssrc;
    this->dst_ssrc = dst_ssrc;
    this->on_recv = [this](const char* data, int length) {
        // 存在CSRC
        uint8_t cc = data[0] & 0x0F;
        if(cc != 0) {
            return;
        }
        // 修改SSRC
        uint32_t* data_ssrc = (uint32_t*) (data + 8);
        uint32_t  ssrc      = ::htonl(this->dst_ssrc);
        std::memcpy(data_ssrc, &ssrc, sizeof(uint32_t));
        // 发送数据
        this->send(this->rtp_addr, data, length);
    };
}

void GbMediaServer::send(const sockaddr_in& addr, const char* data, int length) {
    #ifdef _WIN32
    ::sendto(this->client, data, length, 0, (const sockaddr*) &addr, sizeof(addr));
    #else
    ::sendto(this->client, data, length, 0, (const sockaddr*) &addr, sizeof(addr));
    #endif
}

static void rtp_encode_free(void* param, void* packet) {
//  delete[] (char*) packet;
}

static void* rtp_encode_alloc(void* param, int length) {
    GbMediaServer* server = (GbMediaServer*) param;
    return server->encode_buffer + 4;
//  return new char[length];
}

static int rtp_encode_packet(void* param, const void* packet, int length, uint32_t timestamp, int flags) {
    GbMediaServer* server = (GbMediaServer*) param;
    server->send(server->rtp_addr, (const char*) packet, length);
	return 0;
}

static int rtp_demuxer_packet(void* param, const void* packet, int length, uint32_t timestamp, int flags) {
    GbMediaServer* server = (GbMediaServer*) param;
    int ret = ps_demuxer_input(server->ps_demuxer, (const uint8_t*) packet, length);
    if(ret >= 0 && ret < length) {
//	    std::memcpy(cache, data + ret, length - ret);
    }
    return 0;
}

static int ps_demuxer_packet(void* param, int stream, int codecid, int flags, int64_t pts, int64_t dts, const void* data, size_t length) {
    GbMediaServer* server = (GbMediaServer*) param;
    if (codecid == PSI_STREAM_AUDIO_G711A || codecid == PSI_STREAM_AUDIO_G711U || codecid == PSI_STREAM_MPEG4_AAC) {
        // TODO 验证
        rtp_payload_encode_input(server->audio_encoder, data, length, pts * 8000 / 90000);
    } else if (codecid == PSI_STREAM_H264) {
        static uint32_t v_t = 0;
        rtp_payload_encode_input(server->video_encoder, data, length, pts);
    }
    return 0;
}

extern "C" {

JNIEXPORT void JNICALL Java_com_acgist_taoyao_signal_client_gb_GbMediaServer_init(JNIEnv* env, jclass clazz) {
    #ifdef _WIN32
    WSADATA wsa;
    WSAStartup(MAKEWORD(2, 2), &wsa);
    #endif
}

JNIEXPORT void JNICALL Java_com_acgist_taoyao_signal_client_gb_GbMediaServer_cleanup(JNIEnv* env, jclass clazz) {
    #ifdef _WIN32
    WSACleanup();
    #endif
}

JNIEXPORT jint JNICALL Java_com_acgist_taoyao_signal_client_gb_GbMediaServer_recv(JNIEnv* env, jclass clazz, jstring id, jstring type) {
    jint ret = 0;
    if (id == nullptr || type == nullptr) {
        return -1;
    }
    const char* id_   = env->GetStringUTFChars(id,   nullptr);
    const char* type_ = env->GetStringUTFChars(type, nullptr);
    {
        std::lock_guard<std::mutex> lock(gb_media_server_map_mtx);
        auto iter = gb_media_server_map.find(id_);
        if (iter != gb_media_server_map.end()) {
            ret = iter->second->local_port;
        } else {
            GbMediaServer* server = new GbMediaServer();
            server->recv(0);
            gb_media_server_map.emplace(id_, server);
            ret = server->local_port;
        }
    }
    env->ReleaseStringUTFChars(id,   id_);
    env->ReleaseStringUTFChars(type, type_);
    return ret;
}

JNIEXPORT jint JNICALL Java_com_acgist_taoyao_signal_client_gb_GbMediaServer_send(JNIEnv* env, jclass clazz, jstring id, jstring host, jint port, jlong src_ssrc, jlong audio_ssrc, jlong video_ssrc) {
    if (id == nullptr || host == nullptr) {
        return -1;
    }
    jint ret = 0;
    const char* id_   = env->GetStringUTFChars(id,   nullptr);
    const char* host_ = env->GetStringUTFChars(host, nullptr);
    {
        std::lock_guard<std::mutex> lock(gb_media_server_map_mtx);
        auto iter = gb_media_server_map.find(id_);
        if (iter == gb_media_server_map.end()) {
            ret = -1;
        } else {
            iter->second->send(host_, (uint16_t) port, (uint32_t) src_ssrc, (uint32_t) audio_ssrc, (uint32_t) video_ssrc);
        }
    }
    env->ReleaseStringUTFChars(id,   id_);
    env->ReleaseStringUTFChars(host, host_);
    return ret;
}

JNIEXPORT jint JNICALL Java_com_acgist_taoyao_signal_client_gb_GbMediaServer_forward(JNIEnv* env, jclass clazz, jstring id, jstring host, jint port, jlong src_ssrc, jlong dst_ssrc) {
    if (id == nullptr || host == nullptr) {
        return -1;
    }
    jint ret = 0;
    const char* id_   = env->GetStringUTFChars(id, nullptr);
    const char* host_ = env->GetStringUTFChars(host, nullptr);
    {
        std::lock_guard<std::mutex> lock(gb_media_server_map_mtx);
        auto iter = gb_media_server_map.find(id_);
        if (iter == gb_media_server_map.end()) {
            ret = -1;
        } else {
            iter->second->forward(host_, (uint16_t) port, (uint32_t) src_ssrc, (uint32_t) dst_ssrc);
        }
    }
    env->ReleaseStringUTFChars(id,   id_);
    env->ReleaseStringUTFChars(host, host_);
    return ret;
}

JNIEXPORT jint JNICALL Java_com_acgist_taoyao_signal_client_gb_GbMediaServer_close(JNIEnv* env, jclass clazz, jstring id) {
    if (id == nullptr) {
        return -1;
    }
    jint ret = 0;
    const char* id_ = env->GetStringUTFChars(id, nullptr);
    {
        std::lock_guard<std::mutex> lock(gb_media_server_map_mtx);
        auto iter = gb_media_server_map.find(id_);
        if (iter == gb_media_server_map.end()) {
            ret = -1;
        } else {
            GbMediaServer* server = iter->second;
            gb_media_server_map.erase(iter);
            server->running = false;
            if (server->thread.joinable()) {
                server->thread.join();
            }
            delete server;
        }
    }
    env->ReleaseStringUTFChars(id, id_);
    return ret;
}

}

int main() {
    #ifdef _WIN32
    uint32_t ssrc = 10000;
    WSADATA wsa;
    WSAStartup(MAKEWORD(2, 2), &wsa);
    GbMediaServer* server = new GbMediaServer();
    server->recv(28888);
    std::printf("本地端口: %d\n", server->local_port);
    server->send("127.0.0.1", 48888, ssrc, 10001, 10002);
//  server->forward("127.0.0.1", 28888, ssrc, 10001);
    system("pause");
    server->running = false;
    if (server->thread.joinable()) {
        server->thread.join();
    }
    delete server;
    #endif
    return 0;
}
