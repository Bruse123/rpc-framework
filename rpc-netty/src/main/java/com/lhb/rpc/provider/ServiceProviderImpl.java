package com.lhb.rpc.provider;

import com.lhb.rpc.enums.RpcErrorMsg;
import com.lhb.rpc.exception.RpcException;
import com.lhb.rpc.factory.SingletonFactory;
import com.lhb.rpc.register.ServiceRegister;
import com.lhb.rpc.service.RpcServiceProperties;
import com.lhb.rpc.transport.netty.server.NettyServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端注册服务
 * @Author BruseLin
 * @Date 2021/2/9 10:23
 * @Version 1.0
 */
@Slf4j
public class ServiceProviderImpl implements ServiceProvider {

    /**
     * String：服务名，Object：服务对象
     */
    private final Map<String, Object> serviceMap;

    /**
     * 注册服务到注册中心
     */
    private final ServiceRegister serviceRegister;

    public ServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        serviceRegister = SingletonFactory.getSingleInstance(ServiceRegister.class);
    }

    @Override
    public void publishService(Object service, RpcServiceProperties rpcServiceProperties) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            String serviceName = rpcServiceProperties.getServiceName();
            if (!serviceMap.containsKey(serviceName)) {
                serviceMap.put(serviceName, service);
                log.info("Add service: {}", serviceName);
                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, NettyServer.PORT);
                serviceRegister.registerService(rpcServiceProperties.toRpcServiceName(), inetSocketAddress);
                log.info("add serviceAddress:{}", inetSocketAddress.getHostString());
            }
        } catch (UnknownHostException e) {
            log.error("found error in getLocalHost", e);
        }
    }

    @Override
    public Object getService(RpcServiceProperties rpcServiceProperties) {
        Object service = serviceMap.get(rpcServiceProperties.getServiceName());
        if (service == null) {
            throw new RpcException(RpcErrorMsg.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }
}
