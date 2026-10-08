package it.simonvic.giovi.lsp;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import it.simonvic.giovi.antlr.GioviParser.*;
import it.simonvic.giovi.antlr.GioviParserBaseVisitor;
import it.simonvic.giovi.lsp.Environment.EnvironmentFunction;
import it.simonvic.giovi.lsp.Environment.EnvironmentLoop;
import it.simonvic.giovi.lsp.LSVisitResult.Default;
import it.simonvic.giovi.lsp.LSVisitResult.Nothing;
import it.simonvic.giovi.lsp.LSVisitResult.Type;
import it.simonvic.giovi.lsp.LSVisitResult.Type.*;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeCollection.TypeList;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeCollection.TypeMap;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeBool;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeInt;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeNatural;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeReal;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeText;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeProduct.TypeRecord;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeProduct.TypeStruct;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeProduct.TypeTuple;

public class LSVisitor extends GioviParserBaseVisitor<LSVisitResult> {

	private static final Nothing NOTHING = new Nothing();
	private static final Default DEFAULT = new Default();
	private static final TypeAny ANY = new TypeAny();

	private final List<Diagnostic> diagnostics = new ArrayList<>();

	private Environment env;
	private final Environment rootEnv;

	public LSVisitor(Range range) {
		this.rootEnv = new Environment(range);
		this.env = this.rootEnv;
	}

	public List<Diagnostic> getDiagnostics() {
		return diagnostics;
	}

	public Environment getRootEnv() {
		return rootEnv;
	}

	public Environment getCurrentEnv() {
		return env;
	}

	private void setCurrentEnv(Environment env) {
		this.env = env;
	}

	private void enterScope(ParserRuleContext ctx) {
		setCurrentEnv(new Environment(getCurrentEnv(), LSPUtils.range(ctx)));
	}

	private void enterFunctionScope(ParserRuleContext ctx) {
		setCurrentEnv(new EnvironmentFunction(getCurrentEnv(), LSPUtils.range(ctx)));
	}

	private void enterLoopScope(ParserRuleContext ctx) {
		setCurrentEnv(new EnvironmentLoop(getCurrentEnv(), LSPUtils.range(ctx)));
	}

	private void exitScope() {
		setCurrentEnv(getCurrentEnv().parent());
	}


	///////////////////////////////////////////////

	@Override
	public Nothing visitImportStatement(ImportStatementContext ctx) {
		for (var importEntryCtx : ctx.importEntry()) {
			visit(importEntryCtx);
		}
		return NOTHING;
	}

	@Override
	public Nothing visitImportEntry(ImportEntryContext ctx) {
		// TODO: ...
		// if (ctx.alias != null) {
		// 	getCurrentEnv().declareModule(
		// 		ctx.identifier().getText(), new TypeModule(new Environment(this.env))
		// 	);
		// }
		return NOTHING;
	}

	///////////////////////////////////////////////

	@Override
	public Nothing visitAliasStatement(AliasStatementContext ctx) {
		String alias = ctx.alias.getText();
		var expressionType = visitExpression(ctx.expression());
		var env = getCurrentEnv();
		switch (expressionType) {
			case TypeFunction t -> env.declareFunction(alias, t);
			case TypeType t -> env.declareType(alias, t);
			case TypeModule t -> env.declareModule(alias, t);
			// TODO: vars and consts
			default -> diagnostics.add(
				new Diagnostic(
					LSPUtils.range(ctx.expression()),
					"Can't create an alias for " + expressionType
				)
			);
		}
		return NOTHING;
	}


	///////////////////////////////////////////////


	@Override
	public Nothing visitIfStatement(IfStatementContext ctx) {
		enterScope(ctx.consequence); // needed for pattern matching?
		if (!(visit(ctx.condition) instanceof TypeBool)) {
			diagnostics.add(
				new Diagnostic(
					LSPUtils.range(ctx.condition),
					"A condition must be a boolean value"
				)
			);
		}
		visitStatementOrBlock(ctx.consequence);
		exitScope();
		if (ctx.alternative != null) {
			enterScope(ctx.alternative);
			visitStatementOrBlock(ctx.alternative);
			exitScope();
		}
		return NOTHING;
	}

	@Override
	public Nothing visitMultiIfStatementSubjected(MultiIfStatementSubjectedContext ctx) {
		// TODO: ...
		super.visitMultiIfStatementSubjected(ctx);
		return NOTHING;
	}

	@Override
	public Nothing visitMultiIfStatementSubjectless(MultiIfStatementSubjectlessContext ctx) {
		// TODO: ...
		super.visitMultiIfStatementSubjectless(ctx);
		return NOTHING;
	}

	@Override
	public Nothing visitForStatement(ForStatementContext ctx) {
		// TODO: ...
		return NOTHING;
	}

	@Override
	public Nothing visitRepeatStatement(RepeatStatementContext ctx) {
		if (ctx.IF() != null) {
			if (!(visitExpression(ctx.expression()) instanceof TypeBool)) {
				diagnostics.add(
					new Diagnostic(
						LSPUtils.range(ctx.expression()),
						"A condition must be a boolean value"
					)
				);
			}
		}
		enterLoopScope(ctx.statementOrBlock());
		visit(ctx.statementOrBlock());
		exitScope();
		return NOTHING;
	}

	@Override
	public Nothing visitConstantDeclaration(ConstantDeclarationContext ctx) {
		Type declaredType = visitType(ctx.type());
		Type initExpressionType = visitExpression(ctx.value);
		Type widestType;
		try {
			widestType = widestOf(declaredType, initExpressionType);
			if (!getCurrentEnv().declareConstant(ctx.identifier().getText(), widestType)) {
				diagnostics.add(
					new Diagnostic(
						LSPUtils.range(ctx.identifier()),
						"Constant already declared with name " + ctx.identifier().getText()
					)
				);
			}
		} catch (Exception e) {
			diagnostics.add(new Diagnostic(LSPUtils.range(ctx), "Can't infer type"));
		}
		return NOTHING;
	}

	@Override
	public Nothing visitStructDefinition(StructDefinitionContext ctx) {
		// TODO: ...
		return NOTHING;
	}

	@Override
	public Nothing visitFunctionDeclaration(FunctionDeclarationContext ctx) {
		// TODO: ...
		return NOTHING;
	}

	@Override
	public Type visitInvocation(InvocationContext ctx) {
		var invoked = visitIdentifier(ctx.identifier());
		try {
			return doVisitInvocation(invoked);
		} catch (Exception e) {
			diagnostics.add(
				new Diagnostic(LSPUtils.range(ctx), "Can't invoke a non-function: " + invoked)
			);
			return Type.VOID;
		}
	}

	@Override
	public Type visitExpressionInvocation(ExpressionInvocationContext ctx) {
		var invoked = visitExpression(ctx.expression());
		try {
			return doVisitInvocation(invoked);
		} catch (Exception e) {
			diagnostics.add(
				new Diagnostic(LSPUtils.range(ctx), "Can't invoke a non-function: " + invoked)
			);
			return Type.VOID;
		}
	}

	private Type doVisitInvocation(Type invoked) throws Exception {
		if (invoked instanceof TypeFunction function) {
			return function.signature().returnType();
		} else {
			throw new Exception("Can't invoke a non-function: " + invoked);
		}
	}

	@Override
	public Type visitType(TypeContext ctx) {
		return (Type) super.visitType(ctx);
	}

	@Override
	public Type visitTypeQualified(TypeQualifiedContext ctx) {
		var env = getCurrentEnv();
		for (int i = 0; i < ctx.identifier().size() - 1; i++) { // NOTE: last identifier is the typename
			var moduleIdentifierCtx = ctx.identifier(i);
			String moduleIdentifier = moduleIdentifierCtx.getText();
			var optModule = env.getModule(moduleIdentifier);
			if (optModule.isEmpty()) {
				diagnostics
					.add(new Diagnostic(LSPUtils.range(moduleIdentifierCtx), "Can't find module"));
			} else {
				env = ((TypeModule) optModule.orElseThrow()).module();
			}
		}
		String typename = ctx.typename.getText();
		var optScope = env.getType(typename);
		if (optScope.isEmpty()) {
			diagnostics
				.add(new Diagnostic(LSPUtils.range(ctx.typename), "Type not found " + typename));
			return null;
		}
		return optScope.orElseThrow();
	}

	@Override
	public TypeList visitTypeList(TypeListContext ctx) {
		return new TypeList(visitType(ctx.type()));
	}

	@Override
	public TypeMap visitTypeMap(TypeMapContext ctx) {
		return new TypeMap(visitType(ctx.mapKeyType), visitType(ctx.mapValueType));
	}

	@Override
	public TypeTuple visitTypeTuple(TypeTupleContext ctx) {
		if (ctx.typeTupleEntry() == null) {
			return new TypeTuple();
		}
		var types = new Type[ctx.typeTupleEntry().size()];
		for (int i = 0; i < types.length; i++) {
			types[i] = visitType(ctx.typeTupleEntry(i).type());
		}
		return new TypeTuple(types);
	}

	@Override
	public LSVisitResult visitTypeRecord(TypeRecordContext ctx) {
		return super.visitTypeRecord(ctx);
	}

	@Override
	public Type visitDefaultDefinition(DefaultDefinitionContext ctx) {
		return visitExpression(ctx.expression());
	}

	@Override
	public Nothing visitVarDeclaration(VarDeclarationContext ctx) {
		Type declaredType = visitType(ctx.type());
		Type defaultExpressionType = (ctx.defaultDefinition() != null)
			? visitDefaultDefinition(ctx.defaultDefinition())
			: Type.VOID;
		Type widestType;
		try {
			widestType = widestOf(declaredType, defaultExpressionType);
			if (!getCurrentEnv().declareVariable(ctx.identifier().getText(), widestType)) {
				diagnostics.add(
					new Diagnostic(
						LSPUtils.range(ctx.identifier()),
						"Variable already declared with name " + ctx.identifier().getText()
					)
				);
			}
		} catch (Exception e) {
			diagnostics.add(new Diagnostic(LSPUtils.range(ctx), "Can't infer type"));
		}
		return NOTHING;
	}

	@Override
	public LSVisitResult visitVarDeclarationInfer(VarDeclarationInferContext ctx) {
		Type declaredType = (ctx.type() != null)
			? visitType(ctx.type())
			: Type.VOID;
		Type defaultExpressionType = (ctx.defaultDefinition() != null)
			? visitDefaultDefinition(ctx.defaultDefinition())
			: Type.VOID;
		Type initExpressionType = visitExpression(ctx.expression());
		Type widestType;
		try {
			widestType = widestOf(declaredType, defaultExpressionType, initExpressionType);
			if (!getCurrentEnv().declareVariable(ctx.identifier().getText(), widestType)) {
				diagnostics.add(
					new Diagnostic(
						LSPUtils.range(ctx.identifier()),
						"Variable already declared with name " + ctx.identifier().getText()
					)
				);
			}
		} catch (Exception e) {
			diagnostics.add(new Diagnostic(LSPUtils.range(ctx), "Can't infer type"));
		}
		return NOTHING;
	}

	@Override
	public LSVisitResult visitRedirectionToReservedIdentifier(
		RedirectionToReservedIdentifierContext ctx
	) {
		checkRedirectionToStdInput(ctx.dest);
		return super.visitRedirectionToReservedIdentifier(ctx);
	}

	@Override
	public LSVisitResult visitRedirectionToReservedIdentifierRTL(
		RedirectionToReservedIdentifierRTLContext ctx
	) {
		checkRedirectionToStdInput(ctx.dest);
		return super.visitRedirectionToReservedIdentifierRTL(ctx);
	}

	private void checkRedirectionToStdInput(ReservedIdentifierContext dest) {
		if (dest.getText().equals("_in")) {
			var d = new Diagnostic();
			d.setRange(LSPUtils.range(dest));
			d.setSeverity(DiagnosticSeverity.Error);
			d.setMessage("Can't redirect to standard input");
			diagnostics.add(d);
		}
	}

	private Type visitExpression(ExpressionContext ctx) {
		return (Type) visit(ctx);
	}

	@Override
	public Nothing visitReservedIdentifier(ReservedIdentifierContext ctx) {
		switch (ctx.getText()) {
			case "_in", "_out" -> {}
			case String s -> {
				var d = new Diagnostic();
				d.setRange(LSPUtils.range(ctx));
				d.setMessage("Unknown reserved identifier: " + s);
				d.setSeverity(DiagnosticSeverity.Error);
				diagnostics.add(d);
			}
		}
		return NOTHING;
	}

	@Override
	public TypeTuple visitTupleCreationBody(TupleCreationBodyContext ctx) {
		// TODO: ...
		return new TypeTuple();
	}

	@Override
	public TypeRecord visitRecordCreationBody(RecordCreationBodyContext ctx) {
		// TODO: ...
		return new TypeRecord();
	}

	@Override
	public TypeStruct visitStructCreation(StructCreationContext ctx) {
		// TODO: ...
		return new TypeStruct();
	}

	@Override
	public TypeRange visitRange(RangeContext ctx) {
		var begin = (Type) visit(ctx.start);
		var end = (Type) visit(ctx.end);
		var step = (Type) ((ctx.step != null) ? visit(ctx.step) : Type.VOID);
		try {
			var type = widestOf(begin, end, step);
			return new TypeRange(type);
		} catch (Exception e) {
			diagnostics.add(new Diagnostic(LSPUtils.range(ctx), e.getMessage()));
		}
		return TypeRange.VOID;
	}

	private static Type widestOf(Type... types) throws Exception {
		if (types.length == 0) return Type.VOID;
		Type inferred = null;
		Type next;
		for (var type : types) {
			next = type;
			if (inferred == null) {
				inferred = next;
				continue;
			}
			if (next.equals(inferred) || next.isNarrower(inferred)) {
				continue;
			}
			if (inferred.isNarrower(next)) {
				inferred = next;
				continue;
			}
			// find minimum widest type here or rework widening?
			throw new Exception(
				"Impossible to infer widest type.\n\tExpecting:\t" + inferred
					+ "\n\tbut got:\t" + next
			);
		}
		if (inferred == null) {
			throw new Exception("Impossible to infer widest type");
		}
		return inferred;
	}

	@Override
	public TypeList visitListCreation(ListCreationContext ctx) {
		// TODO: ...
		return new TypeList(Type.VOID);
	}

	@Override
	public TypeMap visitMapCreation(MapCreationContext ctx) {
		// TODO: ...
		return new TypeMap(Type.VOID, Type.VOID);

	}

	@Override
	public Type visitIdentifier(IdentifierContext ctx) {
		String identifier = ctx.getText();
		return getCurrentEnv()
			.lookup(identifier)
			.orElseGet(() -> {
				diagnostics
					.add(
						new Diagnostic(
							LSPUtils.range(ctx),
							"No variable, constant, function, type or module found with name "
								+ identifier
						)
					);
				return null;
			});
	}

	@Override
	public TypeInt visitLiteralInteger(LiteralIntegerContext ctx) {
		return Type.INT;
	}

	@Override
	public TypeNatural visitLiteralNatural(LiteralNaturalContext ctx) {
		return Type.NAT;
	}

	@Override
	public TypeReal visitLiteralReal(LiteralRealContext ctx) {
		return Type.REAL;
	}

	@Override
	public TypeBool visitLiteralBool(LiteralBoolContext ctx) {
		return Type.BOOL;
	}

	@Override
	public TypeText visitLiteralText(LiteralTextContext ctx) {
		return Type.TEXT;
	}

	@Override
	public TypeText visitLiteralTextBlock(LiteralTextBlockContext ctx) {
		return Type.TEXT;
	}

	@Override
	public TypeText visitLiteralExactText(LiteralExactTextContext ctx) {
		return Type.TEXT;
	}

	@Override
	public TypeText visitLiteralExactTextBlock(LiteralExactTextBlockContext ctx) {
		return Type.TEXT;
	}

	@Override
	public Default visitLiteralDefault(LiteralDefaultContext ctx) {
		return DEFAULT;
	}

	@Override
	public TypeAny visitLiteralPlaceholder(LiteralPlaceholderContext ctx) {
		return Type.ANY;
	}

}
