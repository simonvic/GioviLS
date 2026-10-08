package it.simonvic.giovi.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;

public class GioviWorkspaceService implements WorkspaceService {

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		System.out.println("client changed configuration " + params.getSettings());
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		System.out.println("client changed watched files " + params.getChanges());
	}

	@Override
	public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(
		WorkspaceSymbolParams params
	) {
		return CompletableFutures.computeAsync(_ -> {
			return Either.forRight(
				List.of(
					new WorkspaceSymbol(
						"_out", SymbolKind.Namespace,
						Either.forRight(
							new WorkspaceSymbolLocation("file:///home/simonvic/README.md")
						)
					)
				)
			);
		});
	}

}

