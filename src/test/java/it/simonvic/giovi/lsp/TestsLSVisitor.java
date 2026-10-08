package it.simonvic.giovi.lsp;

import java.util.Optional;

import it.simonvic.giovi.lsp.LSVisitResult.Type;

public abstract class TestsLSVisitor extends LSVisitor {

	public TestsLSVisitor() {
		super(null);
	}

	public Optional<Type> lookup(String identifier) {
		return getCurrentEnv().lookup(identifier);
	}

}
