package com.chrisnewland.rest.counterservice.client;

import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CounterClient
{
	private static final Logger logger = LoggerFactory.getLogger(CounterClient.class);

	public static void main(String[] args) throws IOException, InterruptedException
	{
		HttpClient httpClient = HttpClient.newBuilder().build();

		StringBuilder builder = new StringBuilder();

		int expiry = 60;
		String group = "articles";

		for (int data = 0; data < 10000; data++)
		{
			builder.append("group=").append(group).append("&data=").append("1,2,3,4,banana").append("&seconds=").append(expiry);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:8080/count"))
					.headers("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(builder.toString())).build();

			long start = System.nanoTime();
			HttpResponse httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			long end = System.nanoTime();

			logger.info("Response: {}, {} in {}ms", httpResponse.statusCode(), httpResponse.body(), (end-start)/1_000_000d);

			builder.setLength(0);
		}
	}
}
