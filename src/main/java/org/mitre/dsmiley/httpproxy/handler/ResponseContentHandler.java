package org.mitre.dsmiley.httpproxy.handler;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public class ResponseContentHandler {
	/**
	 * Pass the response code. This method with the "reason phrase" is deprecated
	 * but it's the only way to pass the reason along too.
	 * 
	 */
	@SuppressWarnings("deprecation")
	protected void setResponseStatus(HttpResponse proxyResponse, HttpServletResponse servletResponse) {

		servletResponse.setStatus(proxyResponse.getStatusLine().getStatusCode(),
				proxyResponse.getStatusLine().getReasonPhrase());
	}

	/**
	 * Copy response body data (the entity) from the proxy to the servlet client.
	 */
	protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse,
			HttpRequest proxyRequest, HttpServletRequest servletRequest) throws IOException {

		int statusCode = proxyResponse.getStatusLine().getStatusCode();
		if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
			// 304 needs special handling. See:
			// http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
			// Don't send body entity/content!
			servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
		} else {
			HttpEntity entity = proxyResponse.getEntity();
			if (entity != null) {
				OutputStream servletOutputStream = servletResponse.getOutputStream();
				entity.writeTo(servletOutputStream);
			}
		}
	}
}
