package it.simonvic.giovi.lsp.proxy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;

public class GioviTextDocumentServiceProxy implements TextDocumentService {

	private static final Logger log = Logger.getLogger("TextDocumentServiceProxy");

	private final LanguageServer actualServer;

	public GioviTextDocumentServiceProxy(LanguageServer actualServer) {
		this.actualServer = actualServer;
		try {
			var fileHandler = new FileHandler("/tmp/giovils_proxy/textdocument.log", true);
			fileHandler.setFormatter(new SimpleFormatter());
			log.addHandler(fileHandler);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		log.info("init text document service");
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		log.info("client opened " + params.getTextDocument().getUri());
		actualServer.getTextDocumentService().didOpen(params);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		log.info("client changed " + params.getTextDocument().getUri());
		actualServer.getTextDocumentService().didChange(params);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		log.info("client closed " + params.getTextDocument().getUri());
		actualServer.getTextDocumentService().didClose(params);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		log.info("client saved " + params.getTextDocument().getUri());
		actualServer.getTextDocumentService().didSave(params);
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
		CompletionParams completionParams
	) {
		log.info("\tcontext: " + completionParams.getContext());
		return actualServer.getTextDocumentService().completion(completionParams);
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
		CodeActionParams params
	) {
		log.info("client triggered codeActions");
		return actualServer.getTextDocumentService().codeAction(params);
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		log.info("client triggered hover");
		return actualServer.getTextDocumentService().hover(params);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		log.info("client triggered formatting");
		return actualServer.getTextDocumentService().formatting(params);
	}

	@Override
	public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
		log.info("client triggered inlayHint");
		return actualServer.getTextDocumentService().inlayHint(params);
	}

	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
		log.info("client triggered signatureHelp");
		return actualServer.getTextDocumentService().signatureHelp(params);
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
		DocumentSymbolParams params
	) {
		return actualServer.getTextDocumentService().documentSymbol(params);
	}

	@Override
	public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
		return actualServer.getTextDocumentService().diagnostic(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
		DefinitionParams params
	) {
		return actualServer.getTextDocumentService().definition(params);
	}

}
