package it.simonvic.giovi.lsp.proxy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.WorkspaceService;

public class GioviWorkspaceServiceProxy implements WorkspaceService {

	private final LanguageServer actualServer;

	public GioviWorkspaceServiceProxy(LanguageServer actualServer) {
		this.actualServer = actualServer;
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		actualServer.getWorkspaceService().didChangeConfiguration(params);

	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		actualServer.getWorkspaceService().didChangeWatchedFiles(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(
		WorkspaceSymbolParams params
	) {
		return actualServer.getWorkspaceService().symbol(params);
	}

}

