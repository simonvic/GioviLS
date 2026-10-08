package it.simonvic.giovi.lsp;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import it.simonvic.giovi.antlr.GioviExtendedParserBaseVisitor;

public class GioviFormatter extends GioviExtendedParserBaseVisitor<String> {

	private final String content;
	private final Parser parser;
	private final CommonTokenStream tokens;

	public GioviFormatter(String content, Parser parser, CommonTokenStream tokens) {
		this.content = content;
		this.parser = parser;
		this.tokens = tokens;
	}

	@Override
	public String visitTerminal(TerminalNode node) {
		var sb = new StringBuilder();
		if (node.getSymbol().getType() != Token.EOF) {
			sb.append(node.getText());
		}
		for (var hiddenToken : LSPUtils.getHiddenTokensToRight(node, tokens)) {
			sb.append(hiddenToken.getText());
		}
		return sb.toString();
	}

	@Override
	protected String defaultResult() {
		return "";
	}

	@Override
	public String visitErrorNode(ErrorNode node) {
		var sb = new StringBuilder();
		for (var hiddenToken : LSPUtils.getHiddenTokensToLeft(node, tokens)) {
			sb.append(hiddenToken.getText());
		}
		sb.append(node.getText());
		return sb.toString();
	}

	@Override
	protected String aggregateResult(String aggregate, String nextResult) {
		return aggregate + nextResult;
	}

}
