package com.chrisnewland.rest.counterservice.service;

import com.chrisnewland.rest.counterservice.model.IdMap;
import com.chrisnewland.rest.counterservice.model.InMemoryCounter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Path("/")
public class CounterService
{
	private static final InMemoryCounter IN_MEMORY_COUNTER;

	static
	{
		try
		{
			IN_MEMORY_COUNTER = new InMemoryCounter();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String index()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"/static/style.css\"></head>");
		builder.append("<body><h1>CountService</h1>");

		for (Map.Entry<String, IdMap> groupEntry : IN_MEMORY_COUNTER.entrySet())
		{
			String group = groupEntry.getKey();

			IdMap idMap = groupEntry.getValue();

			builder.append("<h3>").append(group).append("</h3>");

			builder.append("<table>");
			builder.append("<tr><th>ID</th><th>Count</th></tr>");

			for (Map.Entry<String, List<Long>> entry : idMap.entrySet())
			{
				String id = entry.getKey();

				List<Long> entries = entry.getValue();

				IN_MEMORY_COUNTER.clean(group, id, entries);

				if (!entries.isEmpty())
				{
					builder.append("<tr><td>").append(id).append("</td><td>").append(entries.size()).append("</td></tr>");
				}
			}

			builder.append("</table>");
		}

		builder.append("</body></html>");

		return builder.toString();
	}

	@GET
	@Path("get/{group}/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public String get(@PathParam("group") String group, @PathParam("id") String id)
	{
		return Integer.toString(IN_MEMORY_COUNTER.getCount(group, id));
	}

	@POST
	@Path("count")
	@Produces(MediaType.TEXT_PLAIN)
	public String count(@FormParam("group") String group, @FormParam("data") String data, @FormParam("seconds") int seconds)
	{
		long expiryMillis = System.currentTimeMillis() + seconds * 1000L;

		IN_MEMORY_COUNTER.countData(group, data, expiryMillis);

		return "OK";
	}
}