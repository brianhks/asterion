package org.asterion.rest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.eclipse.jetty.servlets.GzipFilter;

import javax.ws.rs.core.MediaType;
import java.util.Properties;

/**
 Created by bhawkins on 3/3/15.
 */
public class WebServletModule extends JerseyServletModule
{
	public WebServletModule(Properties props)
	{
	}

	@Override
	protected void configureServlets()
	{
		binder().requireExplicitBindings();
		bind(GuiceFilter.class);

		//Bind web server
		bind(WebServer.class);

		//Bind resource classes here
		bind(EdgeResource.class).in(Scopes.SINGLETON);
		bind(PathResource.class).in(Scopes.SINGLETON);
		bind(VertexResource.class).in(Scopes.SINGLETON);

		bind(GuiceContainer.class);

		ImmutableMap<String, String> params = new ImmutableMap.Builder<String, String>()
				.put("mimeTypes", MediaType.APPLICATION_JSON)
				.put("methods", "GET,POST")
				.build();
		bind(GzipFilter.class).in(Scopes.SINGLETON);
		filter("/*").through(GzipFilter.class, params);

		bind(LoggingFilter.class).in(Scopes.SINGLETON);
		filter("/*").through(LoggingFilter.class);

		// hook Jackson into Jersey as the POJO <-> JSON mapper
		bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
		serve("/*").with(GuiceContainer.class);


	}
}
