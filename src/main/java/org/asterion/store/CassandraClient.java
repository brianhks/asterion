package org.asterion.store;

import com.datastax.driver.core.Session;

import java.io.Closeable;

/**
 Created by bhawkins on 3/4/15.
 */
public interface CassandraClient extends Closeable
{
	/**
	 Returns a session already attached to the keyspace this client was
	 created for.
	 @return
	 */
	public Session getKeyspaceSession();

	public Session getSession();

	public String getKeyspace();

	public void close();
}
