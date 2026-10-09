package it.simonvic.giovi.lsp;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.SyntaxTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.*;

import it.simonvic.giovi.antlr.GioviExtendedLexer;
import it.simonvic.giovi.antlr.GioviExtendedParser;
import it.simonvic.giovi.antlr.GioviExtendedParser.IdentifierContext;

public class LSPUtils {

	/**
	 * Compute the (LSP) range going from the start of one node to the end of another
	 * @param from
	 * @param to
	 * @return range
	 */
	public static Range range(ParserRuleContext from, ParserRuleContext to) {
		if (from == to) {
			return range(from);
		}
		return new Range(
			new Position(from.getStart().getLine() - 1, from.getStart().getCharPositionInLine()),
			new Position(to.getStop().getLine() - 1, to.getStop().getCharPositionInLine())
		);
	}

	/**
	 * Compute the (LSP) range going from the start to the end of a node
	 * @param ctx
	 * @return range
	 */
	public static Range range(ParserRuleContext ctx) {
		return new Range(
			new Position(ctx.getStart().getLine() - 1, ctx.getStart().getCharPositionInLine()),
			new Position(ctx.getStop().getLine() - 1, ctx.getStop().getCharPositionInLine())
		);
	}


	/**
	 * Check if the given position is within the range
	 * @param position
	 * @param range
	 * @return
	 */
	public static boolean isInRange(Position position, Range range) {
		return compare(position, range.getStart()) >= 0
			&& compare(position, range.getEnd()) <= 0;
	}

	/**
	 * Compare two positions
	 * @param x
	 * @param y
	 * @return
	 */
	public static int compare(Position x, Position y) {
		return (x.getLine() != y.getLine())
			? x.getLine() - y.getLine()
			: x.getCharacter() - y.getCharacter();
	}

	/**
	 * Get start position
	 * @param ctx
	 * @return
	 */
	public static Position startPositionOf(IdentifierContext ctx) {
		return new Position(
			ctx.getStart().getLine() - 1,
			ctx.getStart().getCharPositionInLine()
		);
	}

	/**
	 * Get ending position
	 * @param ctx
	 * @return
	 */
	public static Position endPositionOf(IdentifierContext ctx) {
		return new Position(
			ctx.getStop().getLine() - 1,
			ctx.getStop().getCharPositionInLine() + ctx.getText().length()
		);
	}

	/**
	 * Get hidden tokens to the left of the given node up until we see a token on
	 * DEFAULT_TOKEN_CHANNEL.
	 * @param node
	 * @param tokens
	 * @return list of hidden tokens
	 */
	public static List<Token> getHiddenTokensToLeft(SyntaxTree node, CommonTokenStream tokens) {
		var t = tokens.getHiddenTokensToLeft(node.getSourceInterval().a);
		return (t != null) ? t : Collections.emptyList();
	}

	/**
	 * Get hidden tokens to the right of the given node up until we see a token on
	 * DEFAULT_TOKEN_CHANNEL.
	 * @param node
	 * @param tokens
	 * @return list of hidden tokens
	 */
	public static List<Token> getHiddenTokensToRight(SyntaxTree node, CommonTokenStream tokens) {
		var t = tokens.getHiddenTokensToRight(node.getSourceInterval().b);
		return (t != null) ? t : Collections.emptyList();
	}

	/**
	 * Find documentation related to the given tree
	 * @param node
	 * @param tokens
	 * @return optional string
	 */
	public static Optional<String> findDocumentation(SyntaxTree node, CommonTokenStream tokens) {
		List<Token> hiddenTokens = getHiddenTokensToLeft(node, tokens);
		if (hiddenTokens.isEmpty()) {
			return Optional.empty();
		}
		hiddenTokens = hiddenTokens.reversed(); // NOTE: we need tokens from the node, upwards
		Token first = hiddenTokens.getFirst();
		if (first.getType() == GioviExtendedLexer.NEWLINES) {
			first = hiddenTokens.removeFirst();
		}
		if (hiddenTokens.isEmpty()) {
			return Optional.empty();
		}
		hiddenTokens = hiddenTokens.stream()
			.takeWhile(t -> t.getChannel() == GioviExtendedLexer.DOCS)
			.toList();
		if (hiddenTokens.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(
			hiddenTokens.reversed()
				.stream()
				.map(Token::getText)
				.collect(Collectors.joining(""))
		);
	}

	/**
	 * From a node, traverse the the tree upwards until an ancestor of the given type is found
	 * @param <T>
	 * @param leaf
	 * @param klass
	 * @return ancestor or null if the node is the root
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ParserRuleContext> Optional<T> findClosestAncestor(
		ParseTree node,
		Class<T> klass
	) {
		var ancestor = node.getParent();
		while (ancestor != null && !klass.isInstance(ancestor)) {
			ancestor = ancestor.getParent();
		}
		return (ancestor == null)
			? Optional.empty()
			: Optional.of((T) ancestor);
	}

	/**
	 * Get a list of the ancestors ordered from the direct parent to the root of the tree
	 * @param node
	 * @return ancestors
	 */
	public static List<ParseTree> getHierarchy(ParseTree node) {
		var hierarchy = new ArrayList<ParseTree>();
		ParseTree parent = node.getParent();
		while (parent != null) {
			hierarchy.add(parent);
			parent = parent.getParent();
		}
		return hierarchy;
	}

	/**
	 * Find the terminal node at the given position
	 * @param tree
	 * @param position
	 * @return leaf if found
	 */
	public static Optional<TerminalNode> findLeaf(ParseTree tree, Position position) {
		return switch (tree) {
			case TerminalNode leaf -> {
				var token = leaf.getSymbol();
				if (token.getLine() - 1 != position.getLine()) {
					yield Optional.empty();
				}

				int tokenStart = token.getCharPositionInLine();
				int tokenStop = tokenStart + (token.getStopIndex() - token.getStartIndex());
				if (tokenStart <= position.getCharacter() && position.getCharacter() <= tokenStop) {
					yield Optional.of(leaf);
				}
				yield Optional.empty();
			}
			case ParserRuleContext ctx -> {
				if (ctx.getStart().getLine() - 1 > position.getLine()) {
					yield Optional.empty();
				}
				if (
					ctx.getStart().getLine() - 1 == position.getLine()
						&& position.getCharacter() < ctx.getStart().getCharPositionInLine()
				) {
					yield Optional.empty();
				}
				int childCount = ctx.getChildCount();
				for (int i = 0; i < childCount; i++) {
					var childCtx = ctx.getChild(i);
					var result = findLeaf(childCtx, position);
					if (result.isPresent()) {
						yield result;
					}
				}
				yield Optional.empty();
			}
			default -> Optional.empty();
		};
	}

	/**
	 * Get new parser for the given text document
	 * @param textDocument
	 * @return parser
	 * @throws IOException
	 */
	public static GioviExtendedParser parserFrom(TextDocumentIdentifier textDocument)
		throws IOException {
		URI uri = URI.create(textDocument.getUri());
		var lexer = new GioviExtendedLexer(CharStreams.fromFileName(uri.getPath()));
		return new GioviExtendedParser(new CommonTokenStream(lexer));
	}

	/**
	 * Get new parser for the given document
	 * @param doc
	 * @return parser
	 */
	public static GioviExtendedParser parserFrom(GioviDocument doc) {
		var lexer = new GioviExtendedLexer(CharStreams.fromString(doc.getContent()));
		return new GioviExtendedParser(new CommonTokenStream(lexer));
	}

	/**
	 * Get new extended parser for the given content
	 * @param content
	 * @return extended parser
	 */
	public static GioviExtendedParser parserFrom(String content) {
		var lexer = new GioviExtendedLexer(CharStreams.fromString(content));
		return new GioviExtendedParser(new CommonTokenStream(lexer));
	}

	public static final Map<SymbolKind, CompletionItemKind> SYMBOL_TO_COMPLETION_KIND = Map
		.ofEntries(
			Map.entry(SymbolKind.File, /*          => */ CompletionItemKind.File),
			Map.entry(SymbolKind.Module, /*        => */ CompletionItemKind.Module),
			// Map.entry(SymbolKind.Namespace,  /* => */ CompletionItemKind.xxx),
			// Map.entry(SymbolKind.Package,    /* => */ CompletionItemKind.xxx),
			Map.entry(SymbolKind.Class, /*         => */ CompletionItemKind.Class),
			Map.entry(SymbolKind.Method, /*        => */ CompletionItemKind.Method),
			Map.entry(SymbolKind.Property, /*      => */ CompletionItemKind.Property),
			Map.entry(SymbolKind.Field, /*         => */ CompletionItemKind.Field),
			Map.entry(SymbolKind.Constructor, /*   => */ CompletionItemKind.Constructor),
			Map.entry(SymbolKind.Enum, /*          => */ CompletionItemKind.Enum),
			Map.entry(SymbolKind.Interface, /*     => */ CompletionItemKind.Interface),
			Map.entry(SymbolKind.Function, /*      => */ CompletionItemKind.Function),
			Map.entry(SymbolKind.Variable, /*      => */ CompletionItemKind.Variable),
			Map.entry(SymbolKind.Constant, /*      => */ CompletionItemKind.Constant),
			Map.entry(SymbolKind.String, /*        => */ CompletionItemKind.Value),
			Map.entry(SymbolKind.Number, /*        => */ CompletionItemKind.Value),
			Map.entry(SymbolKind.Boolean, /*       => */ CompletionItemKind.Value),
			Map.entry(SymbolKind.Array, /*         => */ CompletionItemKind.Value),
			Map.entry(SymbolKind.Object, /*        => */ CompletionItemKind.Value),
			Map.entry(SymbolKind.Null, /*          => */ CompletionItemKind.Value),
			Map.entry(SymbolKind.EnumMember, /*    => */ CompletionItemKind.EnumMember),
			Map.entry(SymbolKind.Struct, /*        => */ CompletionItemKind.Struct),
			Map.entry(SymbolKind.Event, /*         => */ CompletionItemKind.Event),
			Map.entry(SymbolKind.Operator, /*      => */ CompletionItemKind.Operator),
			Map.entry(SymbolKind.TypeParameter, /* => */ CompletionItemKind.TypeParameter)
		);

	public static CompletionItemKind symbolToCompletionKind(SymbolKind kind) {
		return SYMBOL_TO_COMPLETION_KIND.getOrDefault(kind, CompletionItemKind.Text);
	}

}

