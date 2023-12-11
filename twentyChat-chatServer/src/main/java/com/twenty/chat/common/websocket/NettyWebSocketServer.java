package com.twenty.chat.common.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class NettyWebSocketServer {
    public static final int WEBSOCKET_POET = 8090;
    public static final NettyWebSocketServerHandler NETTY_WEB_SOCKET_SERVER_HANDLER = new NettyWebSocketServerHandler();
    //创建线程池执行器
    //主事件循环组（mainGroup）负责接受客户端连接请求，并将连接分配给工作事件循环组（workerGroup）中的线程进行处理。通常情况下，主事件循环组只需要一个线程即可。
    private EventLoopGroup mainGroup = new NioEventLoopGroup();
    //工作事件循环组（workerGroup）负责实际的I/O操作，例如读取、写入和处理数据。
    // 工作事件循环组的线程数通过NettyRuntime.availableProcessors()来设置为可用的处理器核心数，这样可以充分利用系统资源。
    private EventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors());

    /**
     *  启动 ws sever
     */
    @PostConstruct
    public void start() throws InterruptedException{
        run();
    }

    /**
     * 销毁
     */
    public void destroy() {
        Future<?> futureMainGroup = mainGroup.shutdownGracefully();
        Future<?> futureWorkerGroup = workerGroup.shutdownGracefully();
        futureMainGroup.syncUninterruptibly();
        futureWorkerGroup.syncUninterruptibly();
        log.info("关闭 ws sever 成功！！！");
    }

    public void run() throws InterruptedException{
        //服务器启动引导对象
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(mainGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new LoggingHandler(LogLevel.INFO)) // 为 serverBootstrap 添加日志处理器
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        //30 秒客户端没有向服务端发送心跳则关闭连接
                        pipeline.addLast(new IdleStateHandler(30, 0, 0));
                        // 因为使用http协议， 需要 http解码器
                        pipeline.addLast(new HttpServerCodec());
                        // 以块方式写， 添加 chunkedWriter 处理器
                        pipeline.addLast(new ChunkedWriteHandler());
                        /**
                         * 说明：
                         *  1. http数据在传输过程中是分段的，HttpObjectAggregator可以把多个段聚合起来；
                         *  2. 这就是为什么当浏览器发送大量数据时，就会发出多次 http请求的原因
                         */
                        pipeline.addLast(new HttpObjectAggregator(8192));
                        //保存用户ip
                        pipeline.addLast(new HttpHeadersHandler());
                        /**
                         * 说明：
                         *  1. 对于 WebSocket，它的数据是以帧frame 的形式传递的；
                         *  2. 可以看到 WebSocketFrame 下面有6个子类
                         *  3. 浏览器发送请求时： ws://localhost:7000/hello 表示请求的uri
                         *  4. WebSocketServerProtocolHandler 核心功能是把 http协议升级为 ws 协议，保持长连接；
                         *      是通过一个状态码 101 来切换的
                         */
                        pipeline.addLast(new WebSocketServerProtocolHandler("/"));
                        //自定义handler, 处理业务逻辑
                        pipeline.addLast(NETTY_WEB_SOCKET_SERVER_HANDLER);

                    }
                });
        serverBootstrap.bind(WEBSOCKET_POET).sync();
        System.out.println("启动成功");
    }

}
