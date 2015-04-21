package org.asterion.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.Response;

/**
 Created by bhawkins on 2/17/15.
 */
@Path("/v1/vertex")
public class VertexResource
{

	@POST
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response create()
	{
		return (null);
	}
}
