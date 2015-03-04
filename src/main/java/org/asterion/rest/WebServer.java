package org.asterion.rest;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;
import org.asterion.AsterionException;
import org.asterion.AsterionService;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.asterion.util.Preconditions.checkNotNullOrEmpty;

/**
 Created by bhawkins on 3/3/15.
 */
public class WebServer implements AsterionService
{
	public static final Logger logger = LoggerFactory.getLogger(WebServer.class);

	public static final String JETTY_ADDRESS_PROPERTY = "asterion.jetty.address";
	public static final String JETTY_PORT_PROPERTY = "asterion.jetty.port";
	public static final String JETTY_WEB_ROOT_PROPERTY = "asterion.jetty.static_web_root";
	public static final String JETTY_AUTH_USER_PROPERTY = "asterion.jetty.basic_auth.user";
	public static final String JETTY_AUTH_PASSWORD_PROPERTY = "asterion.jetty.basic_auth.password";
	public static final String JETTY_SSL_PORT = "asterion.jetty.ssl.port";
	public static final String JETTY_SSL_KEYSTORE_PATH = "asterion.jetty.ssl.keystore.path";
	public static final String JETTY_SSL_KEYSTORE_PASSWORD = "asterion.jetty.ssl.keystore.password";

	private InetAddress m_address;
	private int m_port;
	private String m_webRoot;
	private Server m_server;
	private String m_authUser = null;
	private String m_authPassword = null;
	private int m_sslPort;
	private String m_keyStorePath;
	private String m_keyStorePassword;


	public WebServer(int port, String webRoot)
			throws UnknownHostException
	{
		this(null, port, webRoot);
	}

	@Inject
	public WebServer(@Named(JETTY_ADDRESS_PROPERTY) String address,
			@Named(JETTY_PORT_PROPERTY) int port,
			@Named(JETTY_WEB_ROOT_PROPERTY) String webRoot)
			throws UnknownHostException
	{
		checkNotNull(webRoot);

		m_port = port;
		m_webRoot = webRoot;
		m_address = InetAddress.getByName(address);
	}

	@Inject(optional = true)
	public void setAuthCredentials(@Named(JETTY_AUTH_USER_PROPERTY) String user,
			@Named(JETTY_AUTH_PASSWORD_PROPERTY) String password)
	{
		m_authUser = user;
		m_authPassword = password;
	}

	@Inject(optional = true)
	public void setSSLSettings(@Named(JETTY_SSL_PORT) int sslPort,
			@Named(JETTY_SSL_KEYSTORE_PATH) String keyStorePath,
			@Named(JETTY_SSL_KEYSTORE_PASSWORD) String keyStorePassword)
	{
		m_sslPort = sslPort;
		m_keyStorePath = checkNotNullOrEmpty(keyStorePath);
		m_keyStorePassword = checkNotNullOrEmpty(keyStorePassword);
	}

	@Override
	public void start() throws AsterionException
	{
		try
		{
			if (m_port > 0)
				m_server = new Server(new InetSocketAddress(m_address, m_port));
			else
				m_server = new Server();

			//Set up SSL
			if (m_keyStorePath != null && !m_keyStorePath.isEmpty())
			{
				logger.info("Using SSL");
				SslContextFactory sslContextFactory = new SslContextFactory(m_keyStorePath);
				sslContextFactory.setKeyStorePassword(m_keyStorePassword);
				SslSelectChannelConnector selectChannelConnector = new SslSelectChannelConnector(sslContextFactory);
				selectChannelConnector.setPort(m_sslPort);
				m_server.addConnector(selectChannelConnector);
			}

			ServletContextHandler servletContextHandler =
					new ServletContextHandler();

			//Turn on basic auth if the user was specified
			if (m_authUser != null)
			{
				servletContextHandler.setSecurityHandler(basicAuth(m_authUser, m_authPassword, "kairos"));
				servletContextHandler.setContextPath("/");
			}

			servletContextHandler.addFilter(GuiceFilter.class, "/api/*", null);
			servletContextHandler.addServlet(DefaultServlet.class, "/api/*");

			ResourceHandler resourceHandler = new ResourceHandler();
			resourceHandler.setDirectoriesListed(true);
			resourceHandler.setWelcomeFiles(new String[]{"index.html"});
			resourceHandler.setResourceBase(m_webRoot);

			HandlerList handlers = new HandlerList();
			handlers.setHandlers(new Handler[]{servletContextHandler, resourceHandler, new DefaultHandler()});
			m_server.setHandler(handlers);

			m_server.start();
		}
		catch (Exception e)
		{
			throw new AsterionException(e);
		}
	}

	@Override
	public void stop()
	{
		try
		{
			if (m_server != null)
			{
				m_server.stop();
				m_server.join();
			}
		}
		catch (Exception e)
		{
			logger.error("Error stopping web server", e);
		}
	}

	public InetAddress getAddress()
	{
		return m_address;
	}

	private static SecurityHandler basicAuth(String username, String password, String realm)
	{

		HashLoginService l = new HashLoginService();
		l.putUser(username, Credential.getCredential(password), new String[]{"user"});
		l.setName(realm);

		Constraint constraint = new Constraint();
		constraint.setName(Constraint.__BASIC_AUTH);
		constraint.setRoles(new String[]{"user"});
		constraint.setAuthenticate(true);

		ConstraintMapping cm = new ConstraintMapping();
		cm.setConstraint(constraint);
		cm.setPathSpec("/*");

		ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
		csh.setAuthenticator(new BasicAuthenticator());
		csh.setRealmName("myrealm");
		csh.addConstraintMapping(cm);
		csh.setLoginService(l);

		return csh;

	}
}
