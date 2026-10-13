package it.simonvic.giovi.lsp;

import static it.simonvic.giovi.lsp.LSVisitResult.DEFAULT;
import static it.simonvic.giovi.lsp.LSVisitResult.NOTHING;
import static it.simonvic.giovi.lsp.LSVisitResult.Type.widestOf;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.*;

import it.simonvic.giovi.antlr.GioviExtendedLexer;
import it.simonvic.giovi.antlr.GioviExtendedParser.*;
import it.simonvic.giovi.antlr.GioviExtendedParserBaseVisitor;
import it.simonvic.giovi.lsp.Environment.Declaration;
import it.simonvic.giovi.lsp.Environment.EnvironmentFunction;
import it.simonvic.giovi.lsp.Environment.EnvironmentLoop;
import it.simonvic.giovi.lsp.Environment.EnvironmentModule;
import it.simonvic.giovi.lsp.LSVisitResult.*;
import it.simonvic.giovi.lsp.LSVisitResult.Type.*;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeCollection.TypeList;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeCollection.TypeMap;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeFunction.Param;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeBool;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeInt;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeNatural;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeReal;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeText;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeProduct.RecordField;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeProduct.TypeRecord;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeProduct.TypeStruct;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeProduct.TypeTuple;

public class LSVisitor extends GioviExtendedParserBaseVisitor<LSVisitResult> {

	// this is so dumb that I'm actually proud I came up with it
	public static final String NATIVE_MODULES_STUBS = """
		module giovi {
			module util {
				module Random {
					integer() => Int {}
					real() => Real {}
					bool() => Bool {}
					text() => Text {}
				}
				module Math {
					const E Real <- 0r;
					const PI Real <- 0r;
					const TAU Real <- 0r;
					floor(x Real) => Real {}
					ceil(x Real) => Real {}
					abs(x Real) => Real {}
				}
				module Color {
					struct Rgb <
						r Int default 255,
						g Int default 255,
						b Int default 255
					>
					Rgb + struct Rgba <
						a Int default 255
					>
					const WHITE Int <- 0xFFFFFF;
					rgb(r Int, g Int, b Int) => Rgb {}
					rgba(r Int, g Int, b Int, a Int) => Rgba {}
				}
				module Ansi {
					const ESC Text <- ""
					moveTo(line Int, column Int) => Text {}
					moveToColumn(x Int) => Text {}
					moveUp(x Int) => Text {}
					moveDown(x Int) => Text {}
					moveRight(x Int) => Text {}
					moveLeft(x Int) => Text {}
					erase(mode Int) => Text {}
					sized(numerator Int, denominator Int, text Text) => Text {}
					fg(color giovi.util.Color.Rgb) => Text {}
					bg(color giovi.util.Color.Rgb) => Text {}
					styleReset() => Text {}
					bold() => Text {}
					dim() => Text {}
					italic() => Text {}
					underline() => Text {}
					blink() => Text {}
					inverse() => Text {}
					hidden() => Text {}
					strike() => Text {}
				}
			}
		}
		""";

	private final List<Diagnostic> diagnostics = new ArrayList<>();
	private final List<InlayHint> inlayHints = new ArrayList<>();

	private Environment env;
	private final Environment rootEnv;

	public LSVisitor(Range range) {
		this.rootEnv = new Environment(range);
		this.env = this.rootEnv;
		this.visit(LSPUtils.parserFrom(NATIVE_MODULES_STUBS).begin());
	}

	public List<Diagnostic> getDiagnostics() {
		return diagnostics;
	}

	public List<InlayHint> getInlayHints() {
		return inlayHints;
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

	private void enterModuleScope(ParserRuleContext ctx) {
		setCurrentEnv(new EnvironmentModule(getCurrentEnv(), LSPUtils.range(ctx)));
	}

	private void exitScope() {
		setCurrentEnv(getCurrentEnv().parent());
	}

	private void addTypeInlayHint(Type widestType, IdentifierContext ctx) {
		var inlayHint = new InlayHint();
		inlayHint.setKind(InlayHintKind.Type);
		inlayHint.setLabel(switch (widestType) {
			case TypeStruct t -> t.typename();
			default -> widestType.toString();
		});
		inlayHint.setPosition(LSPUtils.endPositionOf(ctx));
		inlayHint.setPaddingLeft(true);
		inlayHints.add(inlayHint);
	}

	private void addDiag(DiagnosticSeverity severity, ParserRuleContext ctx, String message) {
		var diag = new Diagnostic(LSPUtils.range(ctx), message);
		diag.setSeverity(severity);
		diagnostics.add(diag);
	}

	private void addDiagErr(ParserRuleContext ctx, String message) {
		addDiag(DiagnosticSeverity.Error, ctx, message);
	}

	private void addDiagWarn(ParserRuleContext ctx, String message) {
		addDiag(DiagnosticSeverity.Warning, ctx, message);
	}

	@Override
	public LSVisitResult visitModuleDeclaration(ModuleDeclarationContext ctx) {
		enterModuleScope(ctx.body);
		String identifier = ctx.identifier().getText();
		if (
			!getCurrentEnv().parent().declare(
				identifier,
				new Declaration(
					SymbolKind.Module,
					new TypeModule(getCurrentEnv()),
					LSPUtils.range(ctx)
				)
			)
		) {
			addDiagErr(ctx, "Module already declared with name " + identifier);
		}
		visitBlock(ctx.body);
		exitScope();
		return NOTHING;
	}

	@Override
	public Nothing visitImportStatement(ImportStatementContext ctx) {
		for (var importEntryCtx : ctx.importEntry()) {
			visit(importEntryCtx);
		}
		return NOTHING;
	}

	@Override
	public Nothing visitImportEntry(ImportEntryContext ctx) {
		TypeModule t = new TypeModule(this.getCurrentEnv());
		for (var uriCtx : ctx.importURI()) {
			var nextEnv = t.module().getModule(uriCtx.getText());
			if (nextEnv.isEmpty()) {
				addDiagErr(uriCtx, "Can't find module " + uriCtx.getText());
				return NOTHING;
			}
			t = (TypeModule) nextEnv.orElseThrow().type();
		}
		if (ctx.alias != null) {
			String alias = ctx.alias.getText();
			if (
				!getCurrentEnv().declare(
					alias,
					new Declaration(
						SymbolKind.Module,
						t,
						LSPUtils.range(ctx.alias)
					)
				)
			) {
				addDiagErr(ctx.alias, "Module already declared with name " + alias);
			}
		}
		return NOTHING;
	}

	@Override
	public Nothing visitAliasStatement(AliasStatementContext ctx) {
		String alias = ctx.alias.getText();
		var expressionType = visitExpression(ctx.expression());
		var env = getCurrentEnv();
		var pos = LSPUtils.range(ctx.alias);
		switch (expressionType) {
			case TypeFunction t -> env.declare(alias, new Declaration(SymbolKind.Function, t, pos));
			case TypeType t -> env.declare(alias, new Declaration(SymbolKind.Struct, t, pos));
			case TypeProduct t -> env.declare(alias, new Declaration(SymbolKind.Struct, t, pos));
			case TypeModule t -> env.declare(alias, new Declaration(SymbolKind.Module, t, pos));
			// TODO: vars and consts
			default -> addDiagErr(ctx.expression(), "Can't create an alias for " + expressionType);
		}
		return NOTHING;
	}

	@Override
	public Nothing visitIfStatement(IfStatementContext ctx) {
		enterScope(ctx.consequence); // needed for pattern matching?
		if (!(visit(ctx.condition) instanceof TypeBool)) {
			addDiagErr(ctx.condition, "A condition must be a boolean value");
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
				addDiagErr(ctx.expression(), "A condition must be a boolean value");
			}
		}
		enterLoopScope(ctx.statementOrBlock());
		visit(ctx.statementOrBlock());
		exitScope();
		return NOTHING;
	}

	@Override
	public Type visitConstantDeclaration(ConstantDeclarationContext ctx) {
		var idCtx = ctx.identifier();
		String identifier = idCtx.getText();
		var widestType = doVisitConstantDeclaration(ctx);
		if (
			!getCurrentEnv().declare(
				identifier,
				new Declaration(
					SymbolKind.Constant,
					widestType,
					LSPUtils.range(idCtx)
				)
			)
		) {
			addDiagErr(idCtx, "Constant already declared with name " + identifier);
		}
		return widestType;
	}

	private Type doVisitConstantDeclaration(ConstantDeclarationContext ctx) {
		Type declaredType = (ctx.type() == null)
			? Type.VOID
			: visitType(ctx.type());
		Type initExpressionType = visitExpression(ctx.value);
		Type widestType = Type.VOID;
		try {
			widestType = widestOf(declaredType, initExpressionType);
			var idCtx = ctx.identifier();
			if (ctx.type() == null) {
				addTypeInlayHint(widestType, idCtx);
			}
		} catch (Exception e) {
			addDiagErr(ctx, "Can't infer type.\n" + e.getMessage());
		}
		return widestType;
	}

	@Override
	public TypeStruct visitStructDefinition(StructDefinitionContext ctx) {
		// TODO: aggregates
		String typename = ctx.typename.getText();
		var bodyCtx = ctx.structBodyDefinition();

		var fieldsCtxs = bodyCtx.fieldDeclaration();
		var fields = new RecordField[(fieldsCtxs == null) ? 0 : fieldsCtxs.size()];
		for (int i = 0; i < fields.length; i++) {
			var fieldCtx = fieldsCtxs.get(i);
			fields[i] = new RecordField(
				fieldCtx.identifier().getText(),
				visitType(fieldCtx.type())
			);
		}

		var constsCtxs = bodyCtx.constantDeclaration();
		var constants = new RecordField[(constsCtxs == null) ? 0 : constsCtxs.size()];
		for (int i = 0; i < constants.length; i++) {
			var constCtx = constsCtxs.get(i);
			var constType = doVisitConstantDeclaration(constCtx);
			constants[i] = new RecordField(constCtx.identifier().getText(), constType);
		}

		var funcsCtx = bodyCtx.functionDeclaration();
		var functions = new TypeFunction[(funcsCtx == null) ? 0 : funcsCtx.size()];
		for (int i = 0; i < functions.length; i++) {
			functions[i] = doVisitFunctionDeclaration(funcsCtx.get(i));
		}

		var type = new TypeStruct(
			typename,
			fields,
			constants,
			functions
		);
		if (
			!getCurrentEnv().declare(
				typename,
				new Declaration(
					SymbolKind.Struct,
					type,
					LSPUtils.range(ctx.typename)
				)
			)
		) {
			addDiagErr(ctx, "Type already declared with name " + typename);
		}
		return type;
	}

	@Override
	public TypeFunction visitFunctionDeclaration(FunctionDeclarationContext ctx) {
		var type = doVisitFunctionDeclaration(ctx);
		var idCtx = ctx.functionSignature().identifier();
		var identifier = idCtx.getText();
		if (
			!getCurrentEnv().declare(
				identifier,
				new Declaration(
					SymbolKind.Function,
					type,
					LSPUtils.range(idCtx)
				)
			)
		) {
			addDiagErr(ctx, "Function already declared with name " + identifier);
		}
		return type;
	}

	private TypeFunction doVisitFunctionDeclaration(FunctionDeclarationContext ctx) {
		var signatureCtx = ctx.functionSignature();
		var idCtx = signatureCtx.identifier();
		String identifier = idCtx.getText();
		var anonSignatureCtx = signatureCtx.anonFunctionSignature();
		var returnType = (anonSignatureCtx.returnType == null)
			? Type.VOID
			: visitType(anonSignatureCtx.returnType);
		var paramsCtxs = anonSignatureCtx.formalParameters().formalParameter();
		var parameters = new Param[(paramsCtxs == null) ? 0 : paramsCtxs.size()];
		for (int i = 0; i < parameters.length; i++) {
			var paramCtx = paramsCtxs.get(i);
			String name = paramCtx.name.getText();
			var type = visitType(paramCtx.type());
			parameters[i] = (paramCtx.label != null)
				? new Param(paramCtx.label.getText(), name, type)
				: new Param(name, type);
		}
		var type = new TypeFunction(identifier, returnType, parameters);
		enterFunctionScope(ctx.body);
		for (int i = 0; i < parameters.length; i++) {
			var param = parameters[i];
			if (
				!getCurrentEnv().declare(
					param.identifier(),
					new Declaration(
						SymbolKind.Variable,
						param.type(),
						LSPUtils.range(paramsCtxs.get(i).name)
					)
				)
			) {
				addDiagErr(
					paramsCtxs.get(i),
					"Parameter already declared with name " + param.identifier()
				);
			}
		}
		visitBlock(ctx.body);
		exitScope();
		return type;
	}

	@Override
	public TypeFunction visitAnonFunction(AnonFunctionContext ctx) {
		var signatureCtx = ctx.anonFunctionSignature();
		return (signatureCtx == null)
			? TypeFunction.EMPTY
			: visitAnonFunctionSignature(signatureCtx);
	}

	@Override
	public Type visitInvocation(InvocationContext ctx) {
		var invoked = visitIdentifier(ctx.identifier());
		try {
			return doVisitInvocation(invoked);
		} catch (Exception e) {
			addDiagErr(ctx, "Can't invoke a non-function: " + invoked);
			return Type.VOID;
		}
	}

	@Override
	public Type visitExpressionInvocation(ExpressionInvocationContext ctx) {
		var invoked = visitExpression(ctx.expression());
		try {
			return doVisitInvocation(invoked);
		} catch (Exception e) {
			addDiagErr(ctx, "Can't invoke a non-function: " + invoked);
			return Type.VOID;
		}
	}

	private Type doVisitInvocation(Type invoked) throws Exception {
		if (invoked instanceof TypeFunction function) {
			return function.returnType();
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
				addDiagErr(moduleIdentifierCtx, "Can't find module");
			} else {
				env = ((TypeModule) optModule.orElseThrow().type()).module();
			}
		}
		String typename = ctx.typename.getText();
		var optScope = env.getType(typename);
		if (optScope.isEmpty()) {
			addDiagErr(ctx.typename, "Type not found " + typename);
			return null;
		}
		return optScope.orElseThrow().type();
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
	public TypeRecord visitTypeRecord(TypeRecordContext ctx) {
		if (ctx.fieldDeclaration() == null) {
			return new TypeRecord();
		}
		var fields = new RecordField[ctx.fieldDeclaration().size()];
		for (int i = 0; i < fields.length; i++) {
			var fieldCtx = ctx.fieldDeclaration(i);
			fields[i] = new RecordField(
				fieldCtx.identifier().getText(),
				visitType(fieldCtx.type())
			);
		}
		return new TypeRecord(fields);
	}

	@Override
	public TypeFunction visitTypeFunctionRef(TypeFunctionRefContext ctx) {
		return visitAnonFunctionSignature(ctx.anonFunctionSignature());
	}

	@Override
	public TypeFunction visitAnonFunctionSignature(AnonFunctionSignatureContext ctx) {
		var returnType = (ctx.returnType == null)
			? Type.VOID
			: visitType(ctx.returnType);
		var paramsCtxs = ctx.formalParameters().formalParameter();
		var parameters = new Param[(paramsCtxs == null) ? 0 : paramsCtxs.size()];
		for (int i = 0; i < parameters.length; i++) {
			var paramCtx = paramsCtxs.get(i);
			String name = paramCtx.name.getText();
			var type = visitType(paramCtx.type());
			parameters[i] = (paramCtx.label != null)
				? new Param(paramCtx.label.getText(), name, type)
				: new Param(name, type);
		}
		return new TypeFunction("", returnType, parameters);
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
			String name = ctx.name.getText();
			if (
				!getCurrentEnv().declare(
					name,
					new Declaration(
						SymbolKind.Variable,
						widestType,
						LSPUtils.range(ctx.name)
					)
				)
			) {
				addDiagErr(ctx.identifier(), "Variable already declared with name " + name);
			}
		} catch (Exception e) {
			addDiagErr(ctx, "Can't infer type.\n" + e.getMessage());
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
			if (ctx.type() == null) {
				addTypeInlayHint(widestType, ctx.identifier());
			}
			String name = ctx.name.getText();
			if (
				!getCurrentEnv().declare(
					name,
					new Declaration(
						SymbolKind.Variable,
						widestType,
						LSPUtils.range(ctx.name)
					)
				)
			) {
				addDiagErr(ctx.identifier(), "Variable already declared with name " + name);
			}
		} catch (Exception e) {
			addDiagErr(ctx, "Can't infer type.\n" + e.getMessage());
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
	public Type visitExpressionParenthesized(ExpressionParenthesizedContext ctx) {
		return visitExpression(ctx.expression());
	}

	@Override
	public Type visitExpressionCast(ExpressionCastContext ctx) {
		// TODO: check for casting
		return visitType(ctx.type());
	}

	@Override
	public TypeBool visitExpressionTypeCheck(ExpressionTypeCheckContext ctx) {
		return Type.BOOL;
	}

	@Override
	public TypeBool visitExpressionPatternMatching(ExpressionPatternMatchingContext ctx) {
		// TODO: pattern matching
		return Type.BOOL;
	}

	@Override
	public Type visitExpressionDot(ExpressionDotContext ctx) {
		var lhs = visitExpression(ctx.expression());
		if (lhs == null) {
			addDiagErr(ctx, "Error evaluating dot expression");
			return Type.VOID;
		}
		// TODO: properly
		var rhsCtx = ctx.getChild(2);
		var rhs = switch (rhsCtx) {
			case IdentifierContext idCtx -> new MemberKey(idCtx.getText());
			case InvocationContext invocationCtx
				-> new MemberKey(invocationCtx.identifier().getText());
			case ParseTree t -> visit(t);
		};
		var result = lhs.dot(rhs);
		if (rhsCtx instanceof InvocationContext && result instanceof TypeFunction func) {
			return func.returnType();
		}
		return switch (result) {
			case Type t -> t;
			default -> {
				addDiagErr(ctx, "Error evaluating dot expression");
				yield Type.VOID;
			}
		};
	}

	@Override
	public TypeFunction visitExpressionDotRef(ExpressionDotRefContext ctx) {
		// TODO: ...
		return TypeFunction.EMPTY;
	}

	@Override
	public TypeFunction visitExpressionRef(ExpressionRefContext ctx) {
		return switch (visit(ctx.referenceable())) {
			case TypeFunction typeFunc -> typeFunc;
			case null, default -> {
				addDiagErr(ctx.referenceable(), "Can't reference a non-function");
				yield TypeFunction.EMPTY;
			}
		};
	}

	@Override
	public TypeType visitExpressionTypeCreation(ExpressionTypeCreationContext ctx) {
		return new TypeType(visitType(ctx.type()));
	}

	@Override
	public TypeInt visitExpressionBitwiseBool(ExpressionBitwiseBoolContext ctx) {
		return Type.INT;
	}

	@Override
	public TypeInt visitExpressionBitwiseNot(ExpressionBitwiseNotContext ctx) {
		return Type.INT;
	}

	@Override
	public TypeInt visitExpressionBitwiseShift(ExpressionBitwiseShiftContext ctx) {
		return Type.INT;
	}

	@Override
	public TypeBool visitExpressionBool(ExpressionBoolContext ctx) {
		return Type.BOOL;
	}

	@Override
	public TypeBool visitExpressionNot(ExpressionNotContext ctx) {
		return Type.BOOL;
	}

	@Override
	public TypeBool visitExpressionContainment(ExpressionContainmentContext ctx) {
		return Type.BOOL;
	}

	@Override
	public TypeBool visitExpressionEquality(ExpressionEqualityContext ctx) {
		return Type.BOOL;
	}

	@Override
	public TypeBool visitExpressionRelational(ExpressionRelationalContext ctx) {
		return Type.BOOL;
	}

	@Override
	public Type visitExpressionAdditive(ExpressionAdditiveContext ctx) {
		var result = doVisitBinary(
			visitExpression(ctx.expression(0)),
			ctx.bop,
			visitExpression(ctx.expression(1))
		);
		return switch (result) {
			case Type t -> t;
			default -> {
				addDiagErr(ctx, "Can't evaluate expression");
				yield Type.VOID;
			}
		};
	}

	@Override
	public Type visitExpressionMultiplicative(ExpressionMultiplicativeContext ctx) {
		var result = doVisitBinary(
			visitExpression(ctx.expression(0)),
			ctx.bop,
			visitExpression(ctx.expression(1))
		);
		return switch (result) {
			case Type t -> t;
			default -> {
				addDiagErr(ctx, "Can't evaluate expression");
				yield Type.VOID;
			}
		};
	}

	@Override
	public Type visitExpressionPower(ExpressionPowerContext ctx) {
		var result = doVisitBinary(
			visitExpression(ctx.expression(0)),
			ctx.bop,
			visitExpression(ctx.expression(1))
		);
		return switch (result) {
			case Type t -> t;
			default -> {
				addDiagErr(ctx, "Can't evaluate expression");
				yield Type.VOID;
			}
		};
	}

	private LSVisitResult doVisitBinary(Type lhs, Token bop, Type rhs) {
		return switch (bop.getType()) {
			case GioviExtendedLexer.EXPONENTIAL -> lhs.power(rhs);
			case GioviExtendedLexer.MULTIPLICATION -> lhs.multiply(rhs);
			case GioviExtendedLexer.DIVISION -> lhs.divide(rhs);
			case GioviExtendedLexer.MODULO -> lhs.modulo(rhs);
			case GioviExtendedLexer.PLUS -> lhs.plus(rhs);
			case GioviExtendedLexer.MINUS -> lhs.minus(rhs);
			default -> NOTHING;
		};
	}

	@Override
	public Type visitExpressionPrefixed(ExpressionPrefixedContext ctx) {
		var rhs = visitExpression(ctx.expression());
		var result = switch (ctx.prefix.getType()) {
			case GioviExtendedLexer.PLUS -> rhs.positive();
			case GioviExtendedLexer.MINUS -> rhs.negative();
			default -> NOTHING;
		};
		return switch (result) {
			case Type t -> t;
			default -> {
				addDiagErr(ctx, "Can't evaluate expression");
				yield Type.VOID;
			}
		};
	}

	@Override
	public ReservedIdentifier visitReservedIdentifier(ReservedIdentifierContext ctx) {
		return switch (ctx.getText()) {
			case "_out" -> ReservedIdentifier._OUT;
			case "_in" -> ReservedIdentifier._IN;
			case "_count" -> ReservedIdentifier._COUNT;
			case "_range" -> ReservedIdentifier._RANGE;
			case "_chars" -> ReservedIdentifier._CHARS;
			case "_keys" -> ReservedIdentifier._KEYS;
			case "_values" -> ReservedIdentifier._VALUES;
			case String s -> {
				addDiagErr(ctx, "Unknown reserved identifier: " + s);
				yield new ReservedIdentifier(s);
			}
		};
	}

	@Override
	public TypeTuple visitTupleCreationBody(TupleCreationBodyContext ctx) {
		if (ctx.expression() == null) {
			return new TypeTuple();
		}
		var types = new Type[ctx.expression().size()];
		for (int i = 0; i < types.length; i++) {
			types[i] = visitExpression(ctx.expression(i));
		}
		return new TypeTuple(types);
	}

	@Override
	public TypeRecord visitRecordCreationBody(RecordCreationBodyContext ctx) {
		if (ctx.recordEntry() == null) {
			return new TypeRecord();
		}
		var fields = new RecordField[ctx.recordEntry().size()];
		for (int i = 0; i < fields.length; i++) {
			var fieldCtx = ctx.recordEntry(i);
			fields[i] = new RecordField(
				fieldCtx.identifier().getText(),
				visitExpression(fieldCtx.expression())
			);
		}
		return new TypeRecord(fields);
	}

	@Override
	public TypeStruct visitStructCreation(StructCreationContext ctx) {
		var type = visitTypeQualified(ctx.typeQualified());
		if (type instanceof TypeStruct typeStruct) {
			return typeStruct;
		}
		addDiagErr(ctx, "Can't instantiate a non-struct type " + type);
		return new TypeStruct("?");
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
			addDiagErr(ctx, "Can't create range.\n" + e.getMessage());
		}
		return TypeRange.VOID;
	}

	@Override
	public TypeList visitListCreation(ListCreationContext ctx) {
		if (ctx.listEntries().listEntry() == null) return TypeList.VOID;
		Type inferred = null;
		Type next;
		for (var entry : ctx.listEntries().listEntry()) {
			next = switch (entry.getChild(0)) {
				case RangeContext rangeCtx -> visitRange(rangeCtx).type();
				case ExpressionContext exprCtx -> visitExpression(exprCtx);
				default -> null; // unreachable
			};
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
			addDiagErr(
				entry,
				"Impossible to infer widest type.\n\tExpecting:\t" + inferred
					+ "\n\tbut got:\t" + next
			);
		}
		return new TypeList((inferred == null) ? Type.VOID : inferred);
	}

	@Override
	public TypeMap visitMapCreation(MapCreationContext ctx) {
		if (ctx.mapEntries() == null || ctx.mapEntries().mapEntry() == null)
			return TypeMap.VOID_VOID;
		Type inferredKey = null;
		Type nextKey;
		Type inferredVal = null;
		Type nextVal;
		for (var entry : ctx.mapEntries().mapEntry()) {
			nextKey = visitExpression(entry.mapKey);
			if (inferredKey == null) {
				inferredKey = nextKey;
			} else {
				if (!nextKey.equals(inferredKey) && !nextKey.isNarrower(inferredKey)) {
					if (inferredKey.isNarrower(nextKey)) {
						inferredKey = nextKey;
					} else {
						addDiagErr(
							entry,
							"Impossible to infer widest key type.\n\tExpecting:\t" + inferredKey
								+ "\n\tbut got:\t" + nextKey
						);
					}
				}
			}
			nextVal = visitExpression(entry.mapValue);
			if (inferredVal == null) {
				inferredVal = nextVal;
			} else {
				if (!nextVal.equals(inferredVal) && !nextVal.isNarrower(inferredVal)) {
					if (inferredVal.isNarrower(nextVal)) {
						inferredVal = nextVal;
					} else {
						addDiagErr(
							entry,
							"Impossible to infer widest value type.\n\tExpecting:\t" + inferredVal
								+ "\n\tbut got:\t" + nextVal
						);
					}
				}
			}
		}
		return new TypeMap(
			(inferredKey == null) ? Type.VOID : inferredKey,
			(inferredVal == null) ? Type.VOID : inferredVal
		);
	}

	@Override
	public Type visitIdentifier(IdentifierContext ctx) {
		String identifier = ctx.getText();
		return getCurrentEnv()
			.lookup(identifier)
			.map(Declaration::type)
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
