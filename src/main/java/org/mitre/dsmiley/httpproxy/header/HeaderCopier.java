package org.mitre.dsmiley.httpproxy.header;

import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;

public abstract class HeaderCopier {
	/**
	 * These are the "hop-by-hop" headers that should not be copied.
	 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html I use an HttpClient
	 * HeaderGroup class instead of Set&lt;String&gt; because this approach does
	 * case insensitive lookup faster.
	 */
	protected static final HeaderGroup hopByHopHeaders;
	static {
		hopByHopHeaders = new HeaderGroup();
		String[] headers = new String[] { "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE",
				"Trailers", "Transfer-Encoding", "Upgrade" };
		for (String header : headers) {
			hopByHopHeaders.addHeader(new BasicHeader(header, null));
		}
	}

	/** The string prefixing rewritten cookies. */
	protected String getCookieNamePrefix() {
		return "!Proxy!";
	}

}
