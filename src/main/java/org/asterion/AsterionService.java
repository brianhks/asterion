package org.asterion;

/**
 Created by bhawkins on 2/17/15.
 */
public class AsterionService
{
	public interface KairosDBService
	{
		public void start() throws AsterionException;
		public void stop();
	}

}
