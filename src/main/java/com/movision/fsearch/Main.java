package com.movision.fsearch;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * 启动fsearch工程的主程序
 * <p>
 * 服务没起来
 *
 * @Author zhuangyuhao
 * @Date 2017/3/20 17:24
 */
public class Main {

	public static void main(String[] args) throws Exception {
		//启动指定端口的lucene服务
        Server server = new Server(G.getConfig().getInt("port"));    //Server server = new Server(10010);

		//初始化	ServletContextHandler
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.NO_SESSIONS);

		context.setContextPath("/");
		server.setHandler(context);

		// 在ServletContextHandler上面运行SearchServlet
        // http://localhost:10010/fsearch
        context.addServlet(new ServletHolder(new SearchServlet()), "/search");

		server.start();
		server.join();
	}
}
