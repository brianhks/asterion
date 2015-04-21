package org.asterion.store;

import com.datastax.driver.core.Session;
import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 Created by bhawkins on 3/4/15.
 */
public class TestCassandraStore
{

	/*@Rule
	public CassandraCQLUnit m_cassandraCQLUnit = new CassandraCQLUnit(new CQLDataSet()
		{
			@Override
			public List<String> getCQLStatements()
			{
				return Collections.emptyList();
			}

			@Override
			public String getKeyspaceName()
			{
				return "TestKeyspace";
			}

			@Override
			public boolean isKeyspaceCreation()
			{
				return true;
			}

			@Override
			public boolean isKeyspaceDeletion()
			{
				return true;
			}
		});*/

	private CassandraStore m_cassandraStore;
	private CassandraClient m_cassandraClient;

	@Before
	public void connectStore()
	{
		//CassandraStore cassandraStore = new CassandraStore(new CassandraTestClient(m_cassandraCQLUnit));
		m_cassandraClient = new CassandraClientImpl("TestKeyspace", Collections.singletonList("localhost"));
		m_cassandraStore = new CassandraStore(m_cassandraClient);
	}

	@After
	public void removeStore()
	{
		Session session = m_cassandraClient.getSession();
		session.execute("drop keyspace TestKeyspace;");

		m_cassandraStore.close();
	}

	@Test
	public void test_updateVertex()
	{
		ByteBuffer vertexId = ByteBuffer.wrap("vertex_id".getBytes());

		Map<String, String> props = new HashMap<>();
		props.put("name", "bob");
		props.put("height", "6.2");

		m_cassandraStore.updateVertex(vertexId, props);

		assertThat(m_cassandraStore.getVertexProperty(vertexId, "name"), equalTo("bob"));
		assertThat(m_cassandraStore.getVertexProperties(vertexId), equalTo(props));
	}


}
