package com.sample.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

@Configuration
public class AppConfig {

	@Value("${server.baseuri}")
	private String baseURI;

	@Bean
	public WebClient webClient() {
		TcpClient tcpClient = TcpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
				.doOnConnected(conn -> {
					conn.addHandlerLast(new ReadTimeoutHandler(3)).addHandlerLast(new WriteTimeoutHandler(3));
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient))).baseUrl(baseURI).build();

		return webClient;
	}

}
