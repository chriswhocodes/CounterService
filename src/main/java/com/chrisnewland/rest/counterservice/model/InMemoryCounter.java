package com.chrisnewland.rest.counterservice.model;

import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;
import com.chrisnewland.rest.counterservice.dto.CounterDto;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryCounter extends ConcurrentHashMap<String, IdMap>
{
	private static final Logger logger = LoggerFactory.getLogger(InMemoryCounter.class);

	private final CounterDto counterDto;

	public InMemoryCounter() throws SQLException
	{
		counterDto = new CounterDto(Integer.parseInt(System.getProperty("flushMinutes", "1")));

		counterDto.reload(this);
	}

	public int getCount(String group, String id)
	{
		int count = 0;

		IdMap idMap = get(group);

		if (idMap != null)
		{
			List<Long> entries = idMap.get(id);

			if (entries != null)
			{
				clean(group, id, entries);

				count = entries.size();
			}
		}

		return count;
	}

	public String getCount(String group)
	{
		StringBuilder builder = new StringBuilder();

		IdMap idMap = get(group);

		if (idMap != null)
		{
			for (Map.Entry<String, List<Long>> entry : idMap.entrySet())
			{
				String id = entry.getKey();

				List<Long> entries = entry.getValue();

				if (entries != null)
				{
					clean(group, id, entries);

					if (!entries.isEmpty())
					{
						builder.append(id).append('=').append(entries.size()).append(',');
					}
				}
			}

			if (builder.length() > 0)
			{
				builder.deleteCharAt(builder.length() - 1);
			}
		}

		return builder.toString();
	}

	public IdMap getIdMap(String group)
	{
		IdMap idMap = get(group);

		if (idMap == null)
		{
			idMap = new IdMap();
			put(group, idMap);
		}

		return idMap;
	}

	public void countData(String group, String data, long expiryMillis)
	{
		countData(group, data, expiryMillis, false);
	}

	public void countData(String group, String data, long expiryMillis, boolean isLoad)
	{
		//logger.info("countData({},{},{})", group, data, expiryMillis);

		IdMap idMap = getIdMap(group);

		String[] ids = data.split(",");

		for (String id : ids)
		{
			List<Long> entries = idMap.get(id);

			if (entries == null)
			{
				entries = new CopyOnWriteArrayList<>();
				idMap.put(id, entries);
			}

			entries.add(expiryMillis);

			if (!isLoad)
			{
				counterDto.store(group, id, expiryMillis);
			}
		}
	}

	public void clean(String group, String id, List<Long> entries)
	{
		long now = System.currentTimeMillis();

		entries.removeIf(expires -> expires <= now);

		if (entries.isEmpty())
		{
			IdMap idMap = getIdMap(group);

			idMap.remove(id);

			if (idMap.isEmpty())
			{
				remove(group);
			}
		}

		counterDto.deleteOlderThan(group, id, now);
	}
}