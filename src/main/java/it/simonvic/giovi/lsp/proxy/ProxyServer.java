package it.simonvic.giovi.lsp.proxy;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.services.*;

public class ProxyServer implements LanguageServer, LanguageClientAware {

	private final GioviTextDocumentServiceProxy textDocumentService;
	private final GioviWorkspaceServiceProxy workspaceService;
	private LanguageClient proxyClient;
	private boolean shutdownAsked;

	private final LanguageServer actualServer;

	public ProxyServer(LanguageServer actualServer) {
		this.actualServer = actualServer;
		this.textDocumentService = new GioviTextDocumentServiceProxy(actualServer);
		this.workspaceService = new GioviWorkspaceServiceProxy(actualServer);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		return actualServer.initialize(params);
	}

	@Override
	public void initialized(InitializedParams params) {
		actualServer.initialized(params);
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return textDocumentService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return workspaceService;
	}

	@Override
	public void connect(LanguageClient client) {
		this.proxyClient = client;
	}

	@Override
	public void exit() {
		System.exit(shutdownAsked ? 0 : 1);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		this.shutdownAsked = true;
		return CompletableFuture.supplyAsync(() -> null);
	}

}
