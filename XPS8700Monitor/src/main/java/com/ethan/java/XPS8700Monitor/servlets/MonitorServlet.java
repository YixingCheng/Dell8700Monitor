package com.ethan.java.XPS8700Monitor.servlets;


import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ethan.java.XPS8700Monitor.DAO.PageEntityDAO;
import com.ethan.java.XPS8700Monitor.entities.PageEntity;
import com.ethan.java.XPS8700Monitor.utils.NotifUtil;
import com.ethan.java.XPS8700Monitor.utils.PageEntityRetrUtil;

/**
 * Web monitor servlet
 */
public class MonitorServlet extends HttpServlet {
	
	private static final long serialVersionUID = -3884556235221420301L;
	private static final Logger log = 
			Logger.getLogger(MonitorServlet.class.getName());
	
	/* Instances of services needed */
	private PageEntityDAO datastore = null;
	private NotifUtil notifier = null;
	private PageEntityRetrUtil retriever = null;
	
	private int maxRetrievalAttempts = 2;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		/* Get all config parameters */
		String userAgent = config.getInitParameter("retriever.userAgent");
		String senderName = config.getInitParameter("notifier.senderName");
		String senderEmail = config.getInitParameter("notifier.senderEmail");
		
		/* Instantiate all services required */
		this.datastore = PageEntityDAO.getInstance();
		this.notifier = new NotifUtil(senderEmail, senderName);
		this.retriever = new PageEntityRetrUtil(userAgent);
		
		datastore.addUser("waldenlaker@hotmail.com");
		datastore.addUser("r3000.mitbbs@gmail.com");
		try{
			Thread.sleep(1000);
		} catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		
		datastore.addObject("http://outlet.us.dell.com/ARBOnlineSales/Online/InventorySearch.aspx?c=us&cs=22&l=en&s=dfh&brandid=2202&fid=5991");
		try{
			Thread.sleep(1000);
		} catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		
		datastore.addSubscription("waldenlaker@hotmail.com", "http://outlet.us.dell.com/ARBOnlineSales/Online/InventorySearch.aspx?c=us&cs=22&l=en&s=dfh&brandid=2202&fid=5991");
		datastore.addSubscription("r3000.mitbbs@gmail.com", "http://outlet.us.dell.com/ARBOnlineSales/Online/InventorySearch.aspx?c=us&cs=22&l=en&s=dfh&brandid=2202&fid=5991");
		try{
			Thread.sleep(1000);
		} catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		
		log.info("hand-coded here!");
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("Start Web tracker polling servlet");
		run();
	}
	
	
	/**
	 * Run the Web monitor poller
	 */
	private void run() {
		int retrievalAttempts = 0;
		PageEntity oldInstance = null, newInstance = null;
		List<String> registeredUriList = null;
		List<String> subscriberList = null;
		
		registeredUriList = datastore.getAllRegisteredObjects();
		for (String uri : registeredUriList) {
			retrievalAttempts = 0;
			do {
				try {
					newInstance = retriever.retrieveObject(uri);
					break;
				} catch (IOException e) {
					retrievalAttempts++;
					log.warning("I/O issue while trying to retrieve object " 
							+ uri +	" at attempt " + (retrievalAttempts+1) +
							"/" + maxRetrievalAttempts);
				}
			} while(retrievalAttempts < maxRetrievalAttempts);
			if(retrievalAttempts == maxRetrievalAttempts) {
				/* Skip this URI */
				log.warning("Could not retrieve object " 
						+ uri +	" because of repeated I/O errors");
				continue;
			}
			oldInstance = datastore.getMostRecentObjectInstance(uri);
			if(oldInstance == null) {
				/*
				 * This occurs only when the application is launched for the
				 * this is the first polling since the page has been added.
				 * Just add the object instance without sending any notification
				 */
				datastore.addObjectInstance(newInstance);
				continue;
			}

			boolean areObjectsEqual = compareInstances(oldInstance, newInstance);

			if(areObjectsEqual) {
				datastore.updateObjectInstanceTimestamp(oldInstance.getUri(), 
						oldInstance.getTimestamp(), newInstance.getTimestamp());
				continue;
			}
			datastore.addObjectInstance(newInstance);
			subscriberList = datastore.getSubscribers(uri);
			for (String email: subscriberList) {
				try {
					notifier.notifyUser(email, uri);
				} catch (IllegalArgumentException e) {
					log.warning("Could not notify user " + email + " about " +
							"changes to " + uri + ". Error: " + e.getMessage());
				}
			}
		}		
	}

	
	/**
	 * Compare two Web object instances.
	 * 
	 * Check status codes, check when content may be null, two null contents 
	 * may mean the page didn't change anyway.
	 * 
	 * @param a One instance
	 * @param b The other instance
	 * @return true if the two instances match, false otherwise
	 */
	
	private static boolean compareInstances(PageEntity a, PageEntity b) {

		String aContentType = a.getContentType();
		String bContentType = b.getContentType();
		
		int aStatusCode = a.getStatusCode();
		int bStatusCode = b.getStatusCode();
		
		String aContent = a.getContent();
		String bContent = b.getContent();
		
		if(aStatusCode != bStatusCode) {
			return false;
		}
		if(aContentType != null && bContentType != null && 
				!aContentType.equals(bContentType)) {
			return false;
		}
		if(aContent == null && bContent != null) {
			return false;
		}
		if(aContent != null && bContent == null) {
			return false;
		}
		return aContent.equals(bContent);
	}
	
}
