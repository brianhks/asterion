package org.asterion.store;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 Created by bhawkins on 2/17/15.
 */
public class CassandraStore implements DataStore
{
	public static final String CREATE_KEYSPACE = "" +
			"CREATE KEYSPACE IF NOT EXISTS asterion" +
			"  WITH REPLICATION = {'class': 'SimpleStrategy'," +
			"  'replication_factor' : 1}";

	public static final String VERTICES_TABLE = "" +
			"CREATE TABLE IF NOT EXIST vertices (" +
			"  vertex_id blob," +
			"  edge_type string," +

			"  direction int," +
			"  edge_id blob," +

			"  when timestamp," +
			"  PRIMARY KEY ((vertex_id, edge_type), direction, edge_id)";

	public static final String VERTICES_TYPE_TABLES = "" +
			"CREATE TABLE IF NOT EXIST vertices (" +
			"  vertex_id blob," +

			"  vertex_name text static," +
			"  vertex_props text static," +

			"  edge_type string" +

			"  PRIMARY KEY ((vertex_id), edge_type)";


	private final Cluster m_cluster;
	private String m_keyspace;

	public CassandraStore(String keyspace, List<String> nodes)
	{
		final Cluster.Builder builder = new Cluster.Builder()
				.withLoadBalancingPolicy(new TokenAwarePolicy(new DCAwareRoundRobinPolicy()))
				.withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.QUORUM));

		nodes.forEach(n -> builder.addContactPoint(n));

		m_cluster = builder.build();
		m_keyspace = keyspace;

		setupSchema();
	}

	public void updateVertex()
	{
	}


	private void setupSchema()
	{
		try (Session session = m_cluster.connect(m_keyspace))
		{
			session.execute(CREATE_KEYSPACE);
			session.execute(VERTICES_TABLE);
			session.execute(VERTICES_TYPE_TABLES);
		}
	}
}
