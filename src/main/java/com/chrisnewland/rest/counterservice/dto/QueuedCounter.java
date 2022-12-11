package com.chrisnewland.rest.counterservice.dto;

public class QueuedCounter
{
	private String group;
	private String id;
	private long expiryMillis;

	public QueuedCounter(String group, String id, long expiryMillis)
	{
		this.group = group;
		this.id = id;
		this.expiryMillis = expiryMillis;
	}

	public String getGroup()
	{
		return group;
	}

	public String getId()
	{
		return id;
	}

	public long getExpiryMillis()
	{
		return expiryMillis;
	}
}