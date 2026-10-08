package it.simonvic.giovi.lsp;

import static it.simonvic.giovi.lsp.LSVisitorUtils.visit;
import static it.simonvic.giovi.lsp.LSVisitorUtils.parserOf;

import org.junit.jupiter.api.Test;

public class TestLsp {

	@Test
	void foo() {
		var parser = parserOf("""
			i Int
			i Int
			i Int
			i Int
			""");
		System.out.println(parser.begin().program().getStart());
		System.out.println(parser.begin().program().getStop());
		System.out.println(LSPUtils.range(parser.begin().program()));
		System.out.println("");
	}

}
