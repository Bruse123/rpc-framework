package com.lhb.rpc.register.client;

import com.lhb.rpc.enums.SerializationType;
import com.lhb.rpc.factory.SingletonFactory;
import com.lhb.rpc.register.dto.Command;
import com.lhb.rpc.register.handler.RegisterHandler;
import com.lhb.rpc.transport.RpcConstants;
import com.lhb.rpc.transport.command.RpcMessage;
import com.lhb.rpc.transport.command.response.RpcResponse;
import com.lhb.rpc.transport.netty.client.UnProcessedRequests;
import com.lhb.rpc.transport.netty.codec.RpcMessageDecoder;
import com.lhb.rpc.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author BruseLin
 * @Date 2021/2/22 15:30
 * @Version 1.0
 */
@Slf4j
@Component
public class ServiceClient {

    private final UnProcessedRequests unProcessedRequests;

    private final NioEventLoopGroup nioEventLoopGroup;

    private final Bootstrap bootstrap;

    private Channel channel;
    @Value("${registerCenter.ip}")
    private String registerCenterIp;
    @Value("${registerCenter.port}")
    private int registerCenterPort;

    public ServiceClient() {
        this.nioEventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 30 秒之内没有写就发送心跳包
                        pipeline.addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS))
                                .addLast(new RpcMessageEncoder())
                                .addLast(new RpcMessageDecoder())
                                .addLast(new RegisterHandler());
                    }
                });
        this.unProcessedRequests = SingletonFactory.getSingleInstance(UnProcessedRequests.class);
    }

    public CompletableFuture<RpcResponse<Object>> send(RpcMessage message) {
        CompletableFuture<RpcResponse<Object>> responseFuture = new CompletableFuture<>();
        if (message.getMessageType() == RpcConstants.DISCOVERY_TYPE) {
            @SuppressWarnings("unchecked")
            Command<Object> data = (Command<Object>) message.getData();
            unProcessedRequests.putRequest(data.getRequestId(), responseFuture);
        }
        message.setSerializerCodec(SerializationType.KRYO.getCode());
        message.setTransportVersion(RpcConstants.TRANSPORT_VERSION);
        this.channel = getChannel();
        this.channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("send register msg：[{}]", message);
            } else {
                log.error("send register msg fail：", future.cause());
                future.channel().close();
                responseFuture.completeExceptionally(future.cause());
            }
        });
        return responseFuture;
    }

    public Channel getChannel() {
        if (channel == null || !channel.isActive()) {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(registerCenterIp, registerCenterPort);
            channel = doConnect(inetSocketAddress);
        }
        return channel;
    }

    @SneakyThrows
    private Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("connect to RegisterCenter [{}]", inetSocketAddress);
                completableFuture.complete(future.channel());
            }
        });
        return completableFuture.get();
    }

    public void close() {
        this.nioEventLoopGroup.shutdownGracefully();
    }
}
