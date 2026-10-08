package it.simonvic.giovi.lsp;

import static it.simonvic.giovi.lsp.LSPUtils.range;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolKind;

import it.simonvic.giovi.antlr.GioviExtendedParser.*;
import it.simonvic.giovi.antlr.GioviExtendedParserBaseListener;

public class SymbolsListener extends GioviExtendedParserBaseListener {


	private final CommonTokenStream tokens;
	private final Stack<DocumentSymbol> stack = new Stack<>();
	private final List<DocumentSymbol> symbols = new ArrayList<>();

	public SymbolsListener(CommonTokenStream tokens) {
		this.tokens = tokens;
	}

	public List<DocumentSymbol> getSymbols() {
		return symbols;
	}

	private void add(DocumentSymbol documentSymbol) {
		if (stack.isEmpty()) {
			symbols.add(documentSymbol);
			return;
		}
		DocumentSymbol parent = stack.peek();
		if (parent.getChildren() == null) {
			parent.setChildren(new ArrayList<>());
		}
		parent.getChildren().add(documentSymbol);
	}

	private void pushEmptySymbol() {
		var symbol = new DocumentSymbol();
		add(symbol);
		stack.push(symbol);
	}

	@Override
	public void exitVarDeclaration(VarDeclarationContext ctx) {
		add(
			new DocumentSymbol(
				ctx.identifier().getText(),
				SymbolKind.Variable,
				range(ctx),
				range(ctx.identifier()),
				LSPUtils.findDocumentation(ctx, tokens).orElse(null)
			)
		);
	}

	@Override
	public void exitConstantDeclaration(ConstantDeclarationContext ctx) {
		add(
			new DocumentSymbol(
				ctx.name.getText(),
				SymbolKind.Constant,
				range(ctx),
				range(ctx.name),
				LSPUtils.findDocumentation(ctx, tokens).orElse(null)
			)
		);
	}

	@Override
	public void exitImportEntry(ImportEntryContext ctx) {
		if (ctx.alias == null) return;
		add(
			new DocumentSymbol(
				ctx.alias.getText(),
				SymbolKind.Namespace,
				range(ctx),
				range(ctx.alias),
				ctx.importURI().stream().map(c -> c.getText()).collect(Collectors.joining("."))
			)
		);
	}

	@Override
	public void enterModuleDeclaration(ModuleDeclarationContext ctx) {
		pushEmptySymbol();
	}

	@Override
	public void exitModuleDeclaration(ModuleDeclarationContext ctx) {
		var symbol = stack.pop();
		symbol.setName(ctx.identifier().getText());
		symbol.setKind(SymbolKind.Module);
		symbol.setRange(range(ctx));
		symbol.setSelectionRange(range(ctx.identifier()));
		symbol.setDetail(LSPUtils.findDocumentation(ctx, tokens).orElse(null));
	}

	@Override
	public void enterFunctionDeclaration(FunctionDeclarationContext ctx) {
		pushEmptySymbol();
	}

	@Override
	public void exitFunctionDeclaration(FunctionDeclarationContext ctx) {
		var symbol = stack.pop();
		symbol.setName(ctx.functionSignature().identifier().getText());
		symbol.setKind(SymbolKind.Function);
		symbol.setRange(range(ctx));
		symbol.setSelectionRange(range(ctx.functionSignature().identifier()));
		symbol.setDetail(
			LSPUtils.findDocumentation(ctx, tokens)
				.orElse(ctx.functionSignature().getText())
		);
	}

	@Override
	public void enterSelfFunctionDeclaration(SelfFunctionDeclarationContext ctx) {
		pushEmptySymbol();
	}

	@Override
	public void exitSelfFunctionDeclaration(SelfFunctionDeclarationContext ctx) {
		var symbol = stack.pop();
		symbol.setName(ctx.selfFunctionSignature().identifier().getText());
		symbol.setKind(SymbolKind.Function); // Method?
		symbol.setRange(range(ctx));
		symbol.setSelectionRange(range(ctx.selfFunctionSignature().identifier()));
		symbol.setDetail(
			LSPUtils.findDocumentation(ctx, tokens)
				.orElse(ctx.selfFunctionSignature().getText())
		);
	}

	@Override
	public void enterStructDefinition(StructDefinitionContext ctx) {
		pushEmptySymbol();
	}

	@Override
	public void exitStructDefinition(StructDefinitionContext ctx) {
		var symbol = stack.pop();
		symbol.setName(ctx.typename.getText());
		symbol.setKind(SymbolKind.Struct);
		symbol.setRange(range(ctx));
		symbol.setSelectionRange(range(ctx.typename));
		symbol.setDetail(LSPUtils.findDocumentation(ctx, tokens).orElse(null));
	}

	@Override
	public void exitFieldDeclaration(FieldDeclarationContext ctx) {
		add(
			new DocumentSymbol(
				ctx.identifier().getText(),
				SymbolKind.Field,
				range(ctx),
				range(ctx.identifier())
			)
		);
	}


}
