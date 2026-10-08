package it.simonvic.giovi.lsp.proxy;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.lsp4j.launch.LSPLauncher;

public class GioviLSProxy {

	private static final Logger log = Logger.getLogger("GioviLS_Proxy");

	public void run()
		throws IOException, InterruptedException, ExecutionException {

		var logdir = new File("/tmp/giovils_proxy");
		if (!logdir.exists()) {
			logdir.mkdirs();
		}
		var fileHandler = new FileHandler("/tmp/giovils_proxy/giovils.log", true);
		fileHandler.setFormatter(new SimpleFormatter());
		log.addHandler(fileHandler);
		log.info("init GioviLS proxy");

		try (var socket = new Socket("127.0.0.1", 8069)) {
			log.info("Connected to real server socket");
			var proxyClient = new ProxyClient();
			var proxyClientLauncher = LSPLauncher.createClientLauncher(
				proxyClient,
				socket.getInputStream(),
				socket.getOutputStream()
			);

			log.info("Starting proxy server");
			var proxyServer = new ProxyServer(proxyClientLauncher.getRemoteProxy());
			var proxyServerLauncher = LSPLauncher.createServerLauncher(
				proxyServer,
				System.in,
				System.out
			);
			proxyClient.setActualClient(proxyServerLauncher.getRemoteProxy());
			proxyServer.connect(proxyClient);

			log.info("listening for real server");
			Future<Void> clientFuture = proxyClientLauncher.startListening();
			//clientFuture.get();

			log.info("Listening for real client");
			Future<?> serverFuture = proxyServerLauncher.startListening();
			serverFuture.get();

		}

	}

}

