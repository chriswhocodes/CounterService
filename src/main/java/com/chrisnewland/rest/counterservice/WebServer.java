package com.chrisnewland.rest.counterservice;

import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebServer
{
	private static Logger logger = LoggerFactory.getLogger(WebServer.class);

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("WebServer <resources folder>");
			System.exit(-1);
		}

		// Use a custom defined port and hostname
		int port = Integer.parseInt(System.getProperty("port", "8080"));
		String hostname = System.getProperty("host", "127.0.0.1");

		Server server = new Server(new InetSocketAddress(hostname, port));

		ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);

		// =================================================
		// Static file Servlet
		// =================================================
		Path resourcesBase = Paths.get(args[0]);

		Path staticResourcePath = resourcesBase.resolve("static");

		ServletHolder holderStatic = new ServletHolder("static-home", DefaultServlet.class);

		holderStatic.setInitParameter("resourceBase", staticResourcePath.toString());
		holderStatic.setInitParameter("dirAllowed", "true");
		holderStatic.setInitParameter("pathInfoOnly", "true");

		servletContextHandler.addServlet(holderStatic, "/static/*");

		// =================================================
		// Service and Filter Servlet
		// =================================================
		ResourceConfig config = new ResourceConfig();

		String packageWeb = WebServer.class.getPackage()
										   .getName();

		config.packages(packageWeb + ".service");
		config.packages(packageWeb + ".filter");

		ServletHolder servletHolder = new ServletHolder(new ServletContainer(config));
		servletContextHandler.addServlet(servletHolder, "/*");

		// =================================================
		// Session Handling
		// =================================================
		SessionHandler sessionHandler = servletContextHandler.getSessionHandler();
		SessionCache sessionCache = new DefaultSessionCache(sessionHandler);
		sessionCache.setSessionDataStore(new NullSessionDataStore());
		sessionHandler.setSessionCache(sessionCache);

		try
		{
			server.start();
			server.join();
		}
		catch (Throwable t)
		{
			logger.error("Server Error", t);
		}
		finally
		{
			server.destroy();
		}
	}
}