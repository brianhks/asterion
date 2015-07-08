package org.asterion.store;

/**
 Created by bhawkins on 4/3/15.
 */
public enum Direction
{
	IN(1),
	OUT(2),
	BOTH(3);

	private int m_value;

	Direction(int value)
	{
		m_value = value;
	}

	public int opposite()
	{
		if (m_value == 1)
			return (2);
		else if (m_value == 2)
			return (1);
		else
			return (3);
	}

	public int getValue()
	{
		return (m_value);
	}
}
