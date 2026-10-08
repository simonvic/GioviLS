package it.simonvic.giovi.lsp;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GioviLSSocket {

	private static final Logger log = Logger.getLogger("GioviLS");

	public static void main(String[] args) throws IOException {
		var logdir = new File("/tmp/giovils");
		if (!logdir.exists()) {
			logdir.mkdirs();
		}
		var fileHandler = new FileHandler("/tmp/giovils/giovils.log", true);
		fileHandler.setFormatter(new SimpleFormatter());
		log.addHandler(fileHandler);
		log.info("init GioviLS (socket)");
		while (true) {
			try (
				var serverSocket = new ServerSocket(8069);
				var clientSocket = serverSocket.accept();
			) {
				Future<Void> future = GioviLS.launch(
					clientSocket.getInputStream(),
					clientSocket.getOutputStream()
				);
				try {
					System.out.println("futureget: " + future.get());
				} catch (InterruptedException | ExecutionException e) {
					log.info(e.getMessage());
				}
			}
		}
	}

}
