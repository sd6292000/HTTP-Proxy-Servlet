package org.mitre.dsmiley.httpproxy.header;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;

public class RequestHeaderCopier extends HeaderCopier {

	private final boolean doPreserveCookies;
	private final boolean doPreserveHost;
	private final HttpHost targetHost;
	private final boolean doForwardIP;

	public RequestHeaderCopier(boolean doPreserveHost, boolean doPreserveCookies, HttpHost targetHost,
			boolean doForwardIP) {
		this.doPreserveHost = doPreserveHost;
		this.doPreserveCookies = doPreserveCookies;
		this.targetHost = targetHost;
		this.doForwardIP = doForwardIP;
	}

	/**
	 * Copy request headers from the servlet client to the proxy request. This is
	 * easily overridden to add your own.
	 */
	protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
		// Get an Enumeration of all of the header names sent by the client
		@SuppressWarnings("unchecked")
		Enumeration<String> enumerationOfHeaderNames = servletRequest.getHeaderNames();
		while (enumerationOfHeaderNames.hasMoreElements()) {
			String headerName = enumerationOfHeaderNames.nextElement();
			copyRequestHeader(servletRequest, proxyRequest, headerName);
		}
	}

	/**
	 * Copy a request header from the servlet client to the proxy request. This is
	 * easily overridden to filter out certain headers if desired.
	 */
	protected void copyRequestHeader(HttpServletRequest servletRequest, HttpRequest proxyRequest, String headerName) {
		// Instead the content-length is effectively set via InputStreamEntity
		if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH))
			return;
		if (hopByHopHeaders.containsHeader(headerName))
			return;

		@SuppressWarnings("unchecked")
		Enumeration<String> headers = servletRequest.getHeaders(headerName);
		while (headers.hasMoreElements()) {// sometimes more than one value
			String headerValue = headers.nextElement();
			// In case the proxy host is running multiple virtual servers,
			// rewrite the Host header to ensure that we get content from
			// the correct virtual server
			if (!doPreserveHost && headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
				headerValue = targetHost.getHostName();
				if (targetHost.getPort() != -1)
					headerValue += ":" + targetHost.getPort();
			} else if (!doPreserveCookies && headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
				headerValue = getRealCookie(headerValue);
			}
			proxyRequest.addHeader(headerName, headerValue);
		}
	}

	/**
	 * Take any client cookies that were originally from the proxy and prepare them
	 * to send to the proxy. This relies on cookie headers being set correctly
	 * according to RFC 6265 Sec 5.4. This also blocks any local cookies from being
	 * sent to the proxy.
	 */
	protected String getRealCookie(String cookieValue) {
		StringBuilder escapedCookie = new StringBuilder();
		String cookies[] = cookieValue.split("[;,]");
		for (String cookie : cookies) {
			String cookieSplit[] = cookie.split("=");
			if (cookieSplit.length == 2) {
				String cookieName = cookieSplit[0].trim();
				if (cookieName.startsWith(getCookieNamePrefix())) {
					cookieName = cookieName.substring(getCookieNamePrefix().length());
					if (escapedCookie.length() > 0) {
						escapedCookie.append("; ");
					}
					escapedCookie.append(cookieName).append("=").append(cookieSplit[1].trim());
				}
			}
		}
		return escapedCookie.toString();
	}

	public void setXForwardedForHeader(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
		if (doForwardIP) {
			String forHeaderName = "X-Forwarded-For";
			String forHeader = servletRequest.getRemoteAddr();
			String existingForHeader = servletRequest.getHeader(forHeaderName);
			if (existingForHeader != null) {
				forHeader = existingForHeader + ", " + forHeader;
			}
			proxyRequest.setHeader(forHeaderName, forHeader);

			String protoHeaderName = "X-Forwarded-Proto";
			String protoHeader = servletRequest.getScheme();
			proxyRequest.setHeader(protoHeaderName, protoHeader);
		}
	}
}
