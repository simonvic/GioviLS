package it.simonvic.giovi.lsp;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class SyntaxErrorListener extends BaseErrorListener {

	public List<Diagnostic> diagnostics = new ArrayList<>();

	@Override
	public void syntaxError(
		Recognizer<?, ?> recognizer,
		Object offendingSymbol,
		int line,
		int charPositionInLine,
		String msg,
		RecognitionException e
	) {
		var token = (CommonToken) offendingSymbol;
		int startLine = token.getLine();
		int startCol = token.getCharPositionInLine();
		diagnostics.add(
			new Diagnostic(
				new Range(new Position(startLine, startCol), new Position(startLine, startCol)),
				msg
			)
		);
	}

	public List<? extends Diagnostic> getDiagnostics() {
		return diagnostics;
	}

}

