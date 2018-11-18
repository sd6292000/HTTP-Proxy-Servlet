package org.mitre.dsmiley.httpproxy.header;

import java.net.HttpCookie;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;

public class ResponseHeaderCopier extends HeaderCopier {

	private final boolean doPreserveCookies;
	private final String targetUri;

	public ResponseHeaderCopier(boolean doPreserveCookies, String targetUri) {
		this.doPreserveCookies = doPreserveCookies;
		this.targetUri = targetUri;
	}

	/** Copy proxied response headers back to the servlet client. */
	protected void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		for (Header backendRspHeader : proxyResponse.getAllHeaders()) {
			copyResponseHeader(servletRequest, servletResponse, backendRspHeader);
		}
	}

	/**
	 * Copy a proxied response header back to the servlet client. This is easily
	 * overwritten to filter out certain headers if desired.
	 */
	protected void copyResponseHeader(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			Header backendRspHeader) {
		String headerName = backendRspHeader.getName();
		if (hopByHopHeaders.containsHeader(headerName))
			return;
		String headerValue = backendRspHeader.getValue();
		if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE)
				|| headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
			copyProxyCookie(servletRequest, servletResponse, headerValue);
		} else if (headerName.equalsIgnoreCase(HttpHeaders.LOCATION)) {
			// LOCATION Header may have to be rewritten.
			servletResponse.addHeader(headerName, rewriteUrlFromResponse(servletRequest, headerValue));
		} else {
			servletResponse.addHeader(headerName, headerValue);
		}
	}

	/**
	 * For a redirect response from the target server, this translates
	 * {@code theUrl} to redirect to and translates it to one the original client
	 * can use.
	 */
	protected String rewriteUrlFromResponse(HttpServletRequest servletRequest, String theUrl) {

		if (theUrl.startsWith(targetUri)) {
			/*-
			 * The URL points back to the back-end server.
			 * Instead of returning it verbatim we replace the target path with our
			 * source path in a way that should instruct the original client to
			 * request the URL pointed through this Proxy.
			 * We do this by taking the current request and rewriting the path part
			 * using this servlet's absolute path and the path from the returned URL
			 * after the base target URL.
			 */
			StringBuffer curUrl = servletRequest.getRequestURL();// no query
			int pos;
			// Skip the protocol part
			if ((pos = curUrl.indexOf("://")) >= 0) {
				// Skip the authority part
				// + 3 to skip the separator between protocol and authority
				if ((pos = curUrl.indexOf("/", pos + 3)) >= 0) {
					// Trim everything after the authority part.
					curUrl.setLength(pos);
				}
			}
			// Context path starts with a / if it is not blank
			curUrl.append(servletRequest.getContextPath());
			// Servlet path starts with a / if it is not blank
			curUrl.append(servletRequest.getServletPath());
			curUrl.append(theUrl, targetUri.length(), theUrl.length());
			return curUrl.toString();
		}
		return theUrl;
	}

	/**
	 * Copy cookie from the proxy to the servlet client. Replaces cookie path to
	 * local path and renames cookie to avoid collisions.
	 */
	protected void copyProxyCookie(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			String headerValue) {
		// build path for resulting cookie
		String path = servletRequest.getContextPath(); // path starts with / or is empty string
		path += servletRequest.getServletPath(); // servlet path starts with / or is empty string
		if (path.isEmpty()) {
			path = "/";
		}

		for (HttpCookie cookie : HttpCookie.parse(headerValue)) {
			// set cookie name prefixed w/ a proxy value so it won't collide w/ other
			// cookies
			String proxyCookieName = doPreserveCookies ? cookie.getName() : getCookieNamePrefix() + cookie.getName();
			Cookie servletCookie = new Cookie(proxyCookieName, cookie.getValue());
			servletCookie.setComment(cookie.getComment());
			servletCookie.setMaxAge((int) cookie.getMaxAge());
			servletCookie.setPath(path); // set to the path of the proxy servlet
			// don't set cookie domain
			servletCookie.setSecure(cookie.getSecure());
			servletCookie.setVersion(cookie.getVersion());
			servletCookie.setHttpOnly(cookie.isHttpOnly());
			servletResponse.addCookie(servletCookie);
		}
	}

}
