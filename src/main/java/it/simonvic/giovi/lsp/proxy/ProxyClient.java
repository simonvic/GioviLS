package it.simonvic.giovi.lsp.proxy;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

public class ProxyClient implements LanguageClient {

	private LanguageClient actualClient;

	public void setActualClient(LanguageClient actualClient) {
		this.actualClient = actualClient;
	}

	@Override
	public void telemetryEvent(Object object) {
		actualClient.telemetryEvent(object);
	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		actualClient.publishDiagnostics(diagnostics);
	}

	@Override
	public void showMessage(MessageParams messageParams) {
		actualClient.showMessage(messageParams);
	}

	@Override
	public CompletableFuture<MessageActionItem> showMessageRequest(
		ShowMessageRequestParams requestParams
	) {
		return actualClient.showMessageRequest(requestParams);
	}

	@Override
	public void logMessage(MessageParams message) {
		actualClient.logMessage(message);
	}


}
