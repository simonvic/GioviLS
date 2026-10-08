package it.simonvic.giovi.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

public class GioviLanguageServer implements LanguageServer, LanguageClientAware {

	private final GioviTextDocumentService textDocumentService;
	private final GioviWorkspaceService workspaceService;
	private LanguageClient client;
	private boolean shutdownAsked;

	public GioviLanguageServer() {
		this.textDocumentService = new GioviTextDocumentService(this);
		this.workspaceService = new GioviWorkspaceService();
	}

	public LanguageClient getClient() {
		return client;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		var capabilities = new ServerCapabilities();

		capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
		capabilities.setCompletionProvider(new CompletionOptions());
		capabilities.setHoverProvider(new HoverOptions());
		capabilities.setCodeActionProvider(new CodeActionOptions());
		capabilities.setInlayHintProvider(new InlayHintRegistrationOptions());
		capabilities.setSignatureHelpProvider(new SignatureHelpOptions());
		capabilities.setDocumentSymbolProvider(new DocumentSymbolOptions());
		capabilities.setWorkspaceSymbolProvider(new WorkspaceSymbolOptions());
		capabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions());
		capabilities.setDefinitionProvider(new DefinitionOptions());
		capabilities.setWorkspaceSymbolProvider(new WorkspaceSymbolOptions());
		capabilities.setDocumentFormattingProvider(new DocumentFormattingOptions());

		var initResult = new InitializeResult();
		initResult.setCapabilities(capabilities);
		initResult.setServerInfo(new ServerInfo("giovils", "0.1"));
		return CompletableFuture.supplyAsync(() -> initResult);
	}

	@Override
	public void initialized(InitializedParams params) {
		// client.registerCapability(new RegistrationParams(List.of(
		// 	new Registration(
		// 		ServerCapabilitiesConstants.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS,
		// 		"textDocument/publishDiagnostics"))));
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
		this.client = client;
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
