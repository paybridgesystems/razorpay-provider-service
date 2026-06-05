package com.paybridge.payments.client.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignHttpClientConfig {
	
	@Value("${feign.httpclient.max-total-connections:200}")
	private int maxTotalConnections;
	
	@Value("${feign.httpclient.max-connections-per-route:50}")
	private int maxConnectionsPerRoute;
	
	
	@Bean
	PoolingHttpClientConnectionManager connectionManager() {
	    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
	    manager.setMaxTotal(maxTotalConnections);
	    manager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
	    manager.setConnectionConfigResolver(route ->
	            ConnectionConfig.custom()
	                    .setConnectTimeout(Timeout.ofSeconds(5))
	                    .setValidateAfterInactivity(TimeValue.ofSeconds(5))
	                    .build());

	    return manager;
	}
	
	@Bean
	RequestConfig requestConfig() {
	    return RequestConfig.custom()
	            .setConnectionRequestTimeout(Timeout.ofSeconds(5))
	            .setResponseTimeout(Timeout.ofSeconds(30))
	            .build();
	}

	@Bean
	CloseableHttpClient customHttpClient(PoolingHttpClientConnectionManager connectionManager,
	        RequestConfig requestConfig) {

	    return HttpClients.custom()
	            .setConnectionManager(connectionManager)
	            .setDefaultRequestConfig(requestConfig)
	            .evictIdleConnections(TimeValue.ofSeconds(30))
	            .build();
	}
}
