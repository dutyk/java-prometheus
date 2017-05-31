package com.yuankang.java.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;

public class ServletServer {
	private CollectorRegistry collectorRegistry = null;
	public ServletServer(CollectorRegistry collectorRegistry){
		this.collectorRegistry = collectorRegistry;		
	}
	public void startServer() throws Exception{
		Server server = new Server(8080);
	    ServletContextHandler context = new ServletContextHandler();
	    context.setContextPath("/");
	    server.setHandler(context);

	    context.addServlet(new ServletHolder(new MetricsServlet(collectorRegistry)), "/metrics");
	    server.start();
	    server.join();
	}
//	public static void main(String[] args) throws Exception{
//		CollectorRegistry collectorRegistry = new CollectorRegistry();	
//	    Server server = new Server(8080);
//	    ServletContextHandler context = new ServletContextHandler();
//	    context.setContextPath("/");
//	    server.setHandler(context);
//
//	    context.addServlet(new ServletHolder(new MetricsServlet(collectorRegistry)), "/metrics");	
//	    server.start();
//	    server.join();
//	}
}
