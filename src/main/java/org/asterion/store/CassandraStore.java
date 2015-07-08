package org.asterion.store;

import com.datastax.driver.core.*;
import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 Created by bhawkins on 2/17/15.
 */
public class CassandraStore implements DataStore
{
	public static final String CREATE_KEYSPACE = "" +
			"CREATE KEYSPACE IF NOT EXISTS %s" +
			"  WITH REPLICATION = {'class': 'SimpleStrategy'," +
			"  'replication_factor' : 1}";

	/**
	 This holds the edges for a vertex of a specific type
	 */
	public static final String VERTEX_EDGES_TABLE = "" +
			"CREATE TABLE IF NOT EXISTS vertex_edges (" +
			"  vertex_id blob," +
			"  edge_type text," +

			"  direction int," +
			"  edge_id blob," +

			"  when timestamp," +
			"  PRIMARY KEY ((vertex_id, edge_type), direction, edge_id)" +
			")";

	/**
	 Holds properties about a vertex, key/value pairs
	 */
	public static final String VERTICES_TABLES = "" +
			"CREATE TABLE IF NOT EXISTS vertices (" +
			"  vertex_id blob," +
			"  property_name text," +
			"  property_value text," +
			"  PRIMARY KEY ((vertex_id), property_name)" +
			")";

	/**
	 Contains a list of types of edges for a vertex
	 */
	public static final String VERTEX_EDGE_TYPES_TABLE = "" +
			"CREATE TABLE IF NOT EXISTS vertex_edge_types (" +
			"  vertex_id blob," +
			"  edge_type text," +
			"  empty text," +
			"  PRIMARY KEY ((vertex_id), edge_type)" +
			")";

	/**
	 Index of vertices by property
	 */
	public static final String VERTEX_INDICES_TABLE = "" +
			"CREATE TABLE IF NOT EXISTS vertex_indices (" +
			"  property_name text," +
			"  property_value text," +
			"  vertex_id blob," +
			"  empty text," +
			"  PRIMARY KEY ((property_name), property_value, vertex_id)" +
			")";

	private final CassandraClient m_cassandraClient;

	public CassandraStore(CassandraClient cassandraClient)
	{
		m_cassandraClient = cassandraClient;

		setupSchema();
	}

	public void updateVertex(ByteBuffer vertexId, Map<String, String> properties)
	{
		/*
		Typically every vertex will at least have a name property
		We should store the name property in a cache plugin.
		The cache can be used to minimize lookups during queries as
		well as minimize duplicate inserts.
		 */
		try (Session session = m_cassandraClient.getKeyspaceSession())
		{
			//Prepared statements need to be moved to a place where they
			//are only "prepared" once.
			PreparedStatement ps = session.prepare("INSERT INTO vertices (vertex_id, property_name, property_value) VALUES (?, ?, ?);");

			for (String name : properties.keySet())
			{
				BoundStatement bs = new BoundStatement(ps);

				bs.setBytes(0, vertexId);
				bs.setString(1, name);
				bs.setString(2, properties.get(name));

				session.executeAsync(bs);
			}

		}
	}

	public void deleteProperty(ByteBuffer vertexId, String propertyName)
	{
	}

	public void deleteVertex(ByteBuffer vertexId)
	{
	}

	public void addEdge(ByteBuffer sourceVertexId, ByteBuffer destVertexId, Direction direction, String edgeType)
	{
		try (Session session = m_cassandraClient.getKeyspaceSession())
		{
			//Prepared statements need to be moved to a place where they
			//are only "prepared" once.
			PreparedStatement ps = session.prepare("INSERT INTO vertex_edges (vertex_id, edge_type, direction, edge_id, when) " +
					"VALUES (?, ?, ?, ?, ?);");

			BoundStatement bs = new BoundStatement(ps);
			Date now = new Date(System.currentTimeMillis());

			bs.setBytesUnsafe(0, sourceVertexId);
			bs.setString(1, edgeType);
			bs.setInt(2, direction.getValue());
			bs.setBytesUnsafe(3, destVertexId);
			bs.setDate(4, now);

			//todo: look at execute async
			session.execute(bs);

			bs = new BoundStatement(ps);

			bs.setBytesUnsafe(0, destVertexId);
			bs.setString(1, edgeType);
			bs.setInt(2, direction.opposite());
			bs.setBytesUnsafe(3, sourceVertexId);
			bs.setDate(4, now);

			session.execute(bs);
		}
	}

	public void deleteEdge(ByteBuffer sourceVertexId, ByteBuffer destVertexId, String edgeType)
	{
		try (Session session = m_cassandraClient.getKeyspaceSession())
		{

		}
	}

	public Map<String, String> getVertexProperties(ByteBuffer vertexId)
	{
		ImmutableMap.Builder<String, String> ret = new ImmutableMap.Builder<String, String>();

		try (Session session = m_cassandraClient.getKeyspaceSession())
		{
			//Prepared statements need to be moved to a place where they
			//are only "prepared" once.
			PreparedStatement ps = session.prepare("SELECT property_name, property_value FROM vertices WHERE vertex_id = ?;");

			BoundStatement bs = new BoundStatement(ps);

			bs.setBytesUnsafe(0, vertexId);

			ResultSet resultSet = session.execute(bs);

			while (!resultSet.isExhausted())
			{
				Row row = resultSet.one();
				ret.put(row.getString(0), row.getString(1));
			}

		}

		return ret.build();
	}

	public String getVertexProperty(ByteBuffer vertexId, String propertyName)
	{
		String ret = null;

		try (Session session = m_cassandraClient.getKeyspaceSession())
		{
			//Prepared statements need to be moved to a place where they
			//are only "prepared" once.
			PreparedStatement ps = session.prepare("SELECT property_value FROM vertices WHERE vertex_id = ? AND property_name = ?;");

			BoundStatement bs = new BoundStatement(ps);

			bs.setBytesUnsafe(0, vertexId);
			bs.setString(1, propertyName);

			ResultSet resultSet = session.execute(bs);

			ret = resultSet.one().getString(0);

		}

		return ret;
	}

	public List<ByteBuffer> getEdges(ByteBuffer vertexId, String edgeType)
	{
		return null;
	}

	public List<String> getEdgeTypes(ByteBuffer vertexId)
	{
		return null;
	}


	public void close()
	{
	}

	private void setupSchema()
	{
		try (Session session = m_cassandraClient.getSession())
		{
			session.execute(String.format(CREATE_KEYSPACE, m_cassandraClient.getKeyspace()));
		}

		try (Session session = m_cassandraClient.getKeyspaceSession())
		{
			//session.execute(CREATE_KEYSPACE);
			session.execute(VERTEX_EDGES_TABLE);
			session.execute(VERTICES_TABLES);
			session.execute(VERTEX_EDGE_TYPES_TABLE);
			session.execute(VERTEX_INDICES_TABLE);
		}
	}

}
