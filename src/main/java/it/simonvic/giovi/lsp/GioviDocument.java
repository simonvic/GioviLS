package it.simonvic.giovi.lsp;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Position;

public class GioviDocument {

	private final String uri;
	private Integer version;
	private String content;
	private boolean dirtyAST = true;
	private ParseTree ast;
	private List<Diagnostic> diagnostics = new ArrayList<>();
	private List<DocumentSymbol> symbols = new ArrayList<>();
	private LSVisitor visitor;

	public GioviDocument(String uri) {
		this.uri = uri;
	}

	public GioviDocument(String uri, Integer version, String content) {
		this.uri = uri;
		this.version = version;
		this.content = content;
	}

	public String getUri() {
		return uri;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		if (!this.content.equals(content)) {
			this.dirtyAST = true;
			this.content = content;
		}
	}

	public boolean isDirtyAST() {
		return dirtyAST;
	}

	public ParseTree getAST() {
		if (dirtyAST) {
			updateAST();
		}
		return ast;
	}

	public LSVisitor getVisitor() {
		if (dirtyAST) {
			updateAST();
		}
		return visitor;
	}

	public List<DocumentSymbol> getSymbols() {
		if (dirtyAST) {
			updateAST();
		}
		return symbols;
	}

	public List<Diagnostic> getDiagnostics() {
		if (dirtyAST) {
			updateAST();
		}
		return diagnostics;
	}

	public void updateAST() {
		var parser = LSPUtils.parserFrom(content);
		var syntaxErrorsListener = new SyntaxErrorListener();
		var functionsBodyListener = new FunctionsBodyDiagnosticsListener();
		var symbolsListener = new SymbolsListener((CommonTokenStream) parser.getTokenStream());
		parser.removeErrorListeners(); // silences terminal output
		parser.addErrorListener(syntaxErrorsListener);
		parser.addParseListener(functionsBodyListener);
		parser.addParseListener(symbolsListener);
		// these listeners work only with extended parser

		// TODO: use completable future
		ast = parser.begin();

		diagnostics = new ArrayList<>();
		diagnostics.addAll(syntaxErrorsListener.getDiagnostics());
		diagnostics.addAll(functionsBodyListener.getDiagnostics());

		var range = LSPUtils.range(parser.begin());
		range.setStart(new Position(0, 0)); // HACK: why is start wrong?
		visitor = new LSVisitor(range);
		visitor.visit(ast);
		diagnostics.addAll(visitor.getDiagnostics());

		symbols = new ArrayList<>();
		symbols.addAll(symbolsListener.getSymbols());

		dirtyAST = false;
	}

}
