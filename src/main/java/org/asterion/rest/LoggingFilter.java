package org.asterion.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 Created by bhawkins on 3/3/15.
 */
public class LoggingFilter implements Filter
{
	private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		if (log.isDebugEnabled())
		{
			StringBuilder sb = new StringBuilder();

			sb.append(servletRequest.getRemoteAddr()).append(" - ");
			sb.append(((HttpServletRequest)servletRequest).getRequestURI());
			log.debug(sb.toString());
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy()
	{
	}
}
