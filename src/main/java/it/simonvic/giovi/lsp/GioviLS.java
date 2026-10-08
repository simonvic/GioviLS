package it.simonvic.giovi.lsp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.lsp4j.launch.LSPLauncher;

public class GioviLS {

	private static final Logger log = Logger.getLogger("GioviLS");

	public static void main(String[] args)
		throws IOException, InterruptedException, ExecutionException {
		var logdir = new File("/tmp/giovils");
		if (!logdir.exists()) {
			logdir.mkdirs();
		}
		var fileHandler = new FileHandler("/tmp/giovils/giovils.log", true);
		fileHandler.setFormatter(new SimpleFormatter());
		log.addHandler(fileHandler);
		log.info("init GioviLS");
		launch(System.in, System.out);
	}

	public static Future<Void> launch(InputStream in, OutputStream out) {
		var server = new GioviLanguageServer();
		var serverLauncher = LSPLauncher.createServerLauncher(server, in, out);
		server.connect(serverLauncher.getRemoteProxy());
		return serverLauncher.startListening();
	}

}
