package org.mitre.dsmiley.httpproxy.caller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;

public class ReverseproxyCaller {

	private HttpClient proxyClient;
	private boolean doHandleRedirects = false;
	private int connectTimeout = -1;
	private int readTimeout = -1;

	private HttpHost httpHost;

	protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			HttpRequest proxyRequest) throws IOException {

		return proxyClient.execute(httpHost, proxyRequest);
	}

	/**
	 * Called from {@link #init(javax.servlet.ServletConfig)}. HttpClient offers
	 * many opportunities for customization. In any case, it should be thread-safe.
	 */
	protected HttpClient createHttpClient() {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(buildRequestConfig())
				.setDefaultSocketConfig(buildSocketConfig());
		return clientBuilder.build();
	}

	/**
	 * Sub-classes can override specific behaviour of
	 * {@link org.apache.http.client.config.RequestConfig}.
	 */
	protected RequestConfig buildRequestConfig() {
		return RequestConfig.custom()

				.setRedirectsEnabled(doHandleRedirects)

				.setCookieSpec(CookieSpecs.IGNORE_COOKIES) // we handle them in the servlet instead

				.setConnectTimeout(connectTimeout).setSocketTimeout(readTimeout).build();
	}

	/**
	 * Sub-classes can override specific behaviour of
	 * {@link org.apache.http.config.SocketConfig}.
	 */
	protected SocketConfig buildSocketConfig() {

		if (readTimeout < 1) {
			return null;
		}

		return SocketConfig.custom().setSoTimeout(readTimeout).build();
	}
}
