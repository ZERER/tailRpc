package com.tail.rpc.client;

import com.tail.rpc.client.service.LocalServer;
import com.tail.rpc.client.service.ServiceBean;
import com.tail.rpc.constant.RpcConfiguration;
import com.tail.rpc.util.SocketAddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import static com.tail.rpc.constant.RpcConfiguration.ZK_CONNECT_TIME_OUT;
import static com.tail.rpc.constant.RpcConfiguration.ZK_SPILT;

/**
 * @author weidong
 * @date create in 14:20 2018/10/13
 **/
@Slf4j
public class ClientRegister {


    private CuratorFramework zkClient;

    private LocalServer localServer = LocalServer.instance();

    private String zkAddr;

    public ClientRegister(String zkAddr){
        this.zkAddr = zkAddr;
    }

    public void connect(){
        //连接zookeeper
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        zkClient = CuratorFrameworkFactory
                .builder()
                .connectString(zkAddr)
                .namespace(RpcConfiguration.NAME_SPACE)
                .sessionTimeoutMs(ZK_CONNECT_TIME_OUT)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
    }

    /**
     * 从注册中心获取服务
     * @param serverName 注册服务名字
     * @return
     */
    public List<ServiceBean> getServer(String serverName) {
        try {

            String serverNode = ZK_SPILT+serverName;
            List<String> server = zkClient.getChildren().forPath(serverNode);
            List<InetSocketAddress> socketAddresses = SocketAddressUtils.warp(server);
            return localServer.putServer(serverNode, socketAddresses);
        } catch (Exception e) {
            log.error("zookeeper获取服务失败");
            return Collections.emptyList();

        }
    }


    public void close() {
        if (zkClient != null){
            zkClient.close();
        }
    }
}
