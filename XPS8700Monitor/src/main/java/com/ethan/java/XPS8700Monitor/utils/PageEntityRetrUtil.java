package com.ethan.java.XPS8700Monitor.utils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import com.ethan.java.XPS8700Monitor.entities.PageEntity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PageEntityRetrUtil {
	
	private static final Logger log = 
			Logger.getLogger(PageEntityRetrUtil.class.getName());
	private String userAgent = null;

	/**
	 * Constructor
	 * 
	 * @param userAgent The user agent to be used for issuing HTTP requests
	 */
	public PageEntityRetrUtil(String userAgent) {
		this.userAgent = userAgent;
	}
	
	/**
	 * Retrieve an object
	 * 
	 * @param uri The URI of the object
	 * 
	 * @return The instance of the object retrieved
	 * 
	 * @throws IOException if the object cannot be retrieved due to I/O problems
	 */
	public PageEntity retrieveObject(String uri) throws IOException {		
		
		String content = null;
		String contentType = null;
		int statusCode = 0;
		Date timestamp = null;
		
		HttpURLConnection connection = 
				(HttpURLConnection) new URL(uri).openConnection();
		connection.setRequestProperty("User-Agent", this.userAgent);
		connection.setRequestProperty("Connection", "close");
		connection.setInstanceFollowRedirects(true); // it is already the default
		InputStream in = connection.getInputStream();
		contentType = connection.getContentType();
		statusCode  = connection.getResponseCode();
		StringBuilder contentBuilder = new StringBuilder();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));;
	    String currentLine = null;
	    while ((currentLine = reader.readLine()) != null) {
	    	contentBuilder.append(currentLine);
	    }
	    if(in != null) {
	    	in.close();
	    }
	    if (connection != null) {
	    	connection.disconnect();
	    }
		timestamp = new Date();
		
		String rawPage = null;
		rawPage = contentBuilder.toString();
		
		// start to parse the HTML to find the difference
		Document htmldom = Jsoup.parse(rawPage);
		Element targetDiv = htmldom.body().select("div[style*=width: 790px]")
				.first();
		Element numBarEle = targetDiv.select("table div[style*=float:left]")
				.first();
		content = numBarEle.text();
		
		return new PageEntity(uri, content, contentType, 
				timestamp, statusCode);
	}


}
