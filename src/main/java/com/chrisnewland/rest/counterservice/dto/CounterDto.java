package com.chrisnewland.rest.counterservice.dto;

import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;
import com.chrisnewland.rest.counterservice.model.InMemoryCounter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CounterDto
{
	private final Queue<QueuedCounter> insertQueue = new ConcurrentLinkedQueue<>();
	private final Queue<QueuedCounter> deleteQueue = new ConcurrentLinkedQueue<>();

	private final ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();

	private static final Logger logger = LoggerFactory.getLogger(CounterDto.class);

	/*
	DROP USER counter;
	CREATE USER <user> WITH PASSWORD '<password>';

	CREATE TABLE counter(
		groupid text NOT NULL,
		id text NOT NULL,
		expiry bigint NOT NULL);

	GRANT SELECT,INSERT,DELETE ON counter TO counter;
*/

	private static final String TABLE_NAME = "counter";

	private final Connection connection;

	public CounterDto() throws SQLException
	{
		this.connection = DatabaseManager.getConnection();

		singleThreadExecutor.scheduleAtFixedRate(this::flushInsertQueue, 1, 1, TimeUnit.MINUTES);
		singleThreadExecutor.scheduleAtFixedRate(this::flushDeleteQueue, 1, 1, TimeUnit.MINUTES);
	}

	public void store(String group, String id, long expiry)
	{
		insertQueue.add(new QueuedCounter(group, id, expiry));
	}

	public void deleteOlderThan(String group, String id, long expiry)
	{
		deleteQueue.add(new QueuedCounter(group, id, expiry));
	}

	public void reload(InMemoryCounter inMemoryCounter) throws SQLException
	{
		long now = System.currentTimeMillis();

		String sql = String.format("SELECT groupid, id, expiry FROM %s WHERE expiry > ?", TABLE_NAME);

		int loadedCount = 0;

		try (PreparedStatement ps = DatabaseManager.prepare(connection, sql))
		{
			ps.setLong(1, now);

			ResultSet rs = ps.executeQuery();

			while (rs.next())
			{
				String group = rs.getString(1);
				String id = rs.getString(2);
				long expiry = rs.getLong(3);

				inMemoryCounter.countData(group, id, expiry, true);

				loadedCount++;
			}
		}

		logger.info("Loaded {} unexpired counters", loadedCount);
	}

	private void flushInsertQueue()
	{
		if (!insertQueue.isEmpty())
		{
			logger.info("Flushing insert queue of size {}", insertQueue.size());

			String sql = String.format("INSERT INTO %s (groupid,id,expiry) VALUES (?,?,?)", TABLE_NAME);

			try (PreparedStatement ps = DatabaseManager.prepare(connection, sql))
			{
				while (!insertQueue.isEmpty())
				{
					QueuedCounter queuedCounter = insertQueue.poll();

					ps.setString(1, queuedCounter.getGroup());
					ps.setString(2, queuedCounter.getId());
					ps.setLong(3, queuedCounter.getExpiryMillis());

					ps.addBatch();
				}

				ps.executeBatch();
			}
			catch (SQLException e)
			{
				logger.error("Could not flush insert queue", e);
			}
		}
	}

	private void flushDeleteQueue()
	{
		if (!deleteQueue.isEmpty())
		{
			logger.info("Flushing delete queue of size {}", deleteQueue.size());

			String sql = String.format("DELETE FROM %s WHERE groupid=? AND id=? AND expiry <= ?", TABLE_NAME);

			try (PreparedStatement ps = DatabaseManager.prepare(connection, sql))
			{
				while (!deleteQueue.isEmpty())
				{
					QueuedCounter queuedCounter = deleteQueue.poll();

					ps.setString(1, queuedCounter.getGroup());
					ps.setString(2, queuedCounter.getId());
					ps.setLong(3, queuedCounter.getExpiryMillis());

					ps.addBatch();
				}

				ps.executeBatch();
			}
			catch (SQLException e)
			{
				logger.error("Could not flush delete queue", e);
			}
		}
	}
}