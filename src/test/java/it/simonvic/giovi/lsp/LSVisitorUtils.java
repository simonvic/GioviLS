package it.simonvic.giovi.lsp;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import it.simonvic.giovi.antlr.GioviExtendedLexer;
import it.simonvic.giovi.antlr.GioviExtendedParser;

public class LSVisitorUtils {

	private static class DummyTestsVisitor extends TestsLSVisitor {
	}

	public static GioviExtendedParser parserOf(String code) {
		var lexer = new GioviExtendedLexer(CharStreams.fromString(code));
		var tokens = new CommonTokenStream(lexer);
		var parser = new GioviExtendedParser(tokens);
		// parser.addErrorListener(new GioviParserErrorListener(code));
		return parser;
	}

	public static TestsLSVisitor visit(String code) {
		return visit(new DummyTestsVisitor(), code);
	}

	public static LSVisitResult visitExpression(String code) {
		return visitExpression(new DummyTestsVisitor(), code);
	}

	public static TestsLSVisitor visit(TestsLSVisitor visitor, String code) {
		visitor.visit(parserOf(code).begin());
		return visitor;
	}

	public static TestsLSVisitor visit(String code, TestsLSVisitor visitor) {
		visitor.visit(parserOf(code).begin());
		return visitor;
	}

	public static LSVisitResult visitExpression(TestsLSVisitor visitor, String code) {
		return visitor.visit(parserOf(code).expression());
	}

}
