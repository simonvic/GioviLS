package it.simonvic.giovi.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import it.simonvic.giovi.antlr.GioviExtendedParserBaseListener;
import it.simonvic.giovi.antlr.GioviExtendedParser.FunctionDeclarationContext;
import it.simonvic.giovi.antlr.GioviExtendedParser.StatementContext;

public class FunctionsBodyDiagnosticsListener extends GioviExtendedParserBaseListener {

	private final List<Diagnostic> diags = new ArrayList<>();

	@Override
	public void exitFunctionDeclaration(FunctionDeclarationContext ctx) {
		findDeadcode(ctx);
	}

	private void findDeadcode(FunctionDeclarationContext ctx) {
		int statementCounts = ctx.body.statement().size();
		List<StatementContext> deadStatements = new ArrayList<>();
		for (int i = 0; i < statementCounts; i++) {
			var statementCtx = ctx.body.statement(i);
			if (statementCtx.returnStatement() != null && i < statementCounts) {
				for (int j = i + 1; j < statementCounts; j++) {
					deadStatements.add(ctx.body.statement(j));
				}
				break;
			}
		}
		if (!deadStatements.isEmpty()) {
			var diag = new Diagnostic();
			diag.setMessage("Dead code");
			diag.setRange(LSPUtils.range(deadStatements.getFirst(), deadStatements.getLast()));
			diag.setSeverity(DiagnosticSeverity.Hint);
			diags.add(diag);
		}
	}

	public List<? extends Diagnostic> getDiagnostics() {
		return diags;
	}

}

