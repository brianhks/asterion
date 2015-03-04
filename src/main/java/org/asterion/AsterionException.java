package org.asterion;

/**
 Created by bhawkins on 2/17/15.
 */
public class AsterionException extends Exception
{
	public AsterionException(String message)
	{
		super(message);
	}

	public AsterionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public AsterionException(Throwable cause)
	{
		super(cause);
	}
}
