package com.movision.fsearch;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {

	public static void main(String[] args) throws Exception {
		//启动指定端口的lucene服务
		Server server = new Server(G.getConfig().getInt("port"));

		//初始化	ServletContextHandler
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.NO_SESSIONS);

		context.setContextPath("/");
		server.setHandler(context);

		// 在ServletContextHandler上面运行SearchServlet
		context.addServlet(new ServletHolder(new SearchServlet()), "/*");

		server.start();
		server.join();
	}
}
