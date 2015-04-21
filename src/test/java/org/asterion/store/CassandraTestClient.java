package org.asterion.store;

import com.datastax.driver.core.Session;
import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSet;

import java.util.Collections;
import java.util.List;

/**
 Created by bhawkins on 3/4/15.
 */
public class CassandraTestClient implements CassandraClient
{
	private final CassandraCQLUnit m_cassandraCQLUnit;

	public CassandraTestClient(CassandraCQLUnit cassandraCQLUnit)
	{
		m_cassandraCQLUnit = cassandraCQLUnit;
	}

	@Override
	public Session getKeyspaceSession()
	{
		return m_cassandraCQLUnit.session;
	}

	@Override
	public Session getSession()
	{
		return m_cassandraCQLUnit.session;
	}

	@Override
	public String getKeyspace()
	{
		return ("test");
	}

	@Override
	public void close()
	{
	}
}
