package it.simonvic.giovi.lsp;

import java.util.*;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import it.simonvic.giovi.lsp.LSVisitResult.Type;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive;

public sealed class Environment {

	public static final class EnvironmentFunction extends Environment {

		public EnvironmentFunction(Environment parent, Range range) {
			super(parent, range);
		}

		public EnvironmentFunction(Range range) {
			super(range);
		}

	}

	public static final class EnvironmentLoop extends Environment {

		public EnvironmentLoop(Environment parent, Range range) {
			super(parent, range);
		}

		public EnvironmentLoop(Range range) {
			super(range);
		}

	}

	private final Environment parent;
	private final Range range;
	private final List<Environment> children = new ArrayList<>();

	public Environment(Environment parent, Range range) {
		this.parent = parent;
		this.range = range;
		parent.children.add(this);
	}

	public Environment(Range range) {
		this.parent = null;
		this.range = range;
	}

	public Environment parent() {
		return parent;
	}

	public Range range() {
		return range;
	}

	public List<Environment> children() {
		return children;
	}

	/**
	 * Query the smallest environment that contains the given position
	 *
	 * <pre><code>
	 *  -----------------------------
	 *  |                           |
	 *  |    ______                 |
	 *  |   |_____|                 |
	 *  |     ______________        |
	 *  |    |  ____  ____ |        |
	 *  |    | |___| |___| |        |
	 *  |    |_____________|        |
	 *  |                           |
	 *  -----------------------------
	 *  </code></pre>
	 *
	 * @param position
	 * @return
	 */
	public Optional<Environment> query(Position position) {
		// TODO: convert to non-recursive
		if (!LSPUtils.isInRange(position, this.range)) {
			return Optional.empty();
		}
		for (var child : this.children) {
			var optEnv = child.query(position);
			if (optEnv.isPresent()) {
				return optEnv.orElseThrow().query(position);
			}
		}
		return Optional.of(this);
	}

	public Map<String, CompletionItem> lookupAll() {
		var items = new HashMap<String, CompletionItem>();
		// we get values in reverse lookup order, so we get shadowing for free
		// TODO: no, that's stupid, just start from bottom and check if contained already
		if (parent != null) {
			items.putAll(parent.lookupAll());
		}
		modules.forEach((k, v) -> {
			var item = new CompletionItem();
			item.setKind(CompletionItemKind.Module);
			item.setLabel(k);
			items.put(k, item);
		});
		types.forEach((k, v) -> {
			var item = new CompletionItem();
			item.setKind(CompletionItemKind.Class);
			item.setLabel(k);
			items.put(k, item);
		});
		functions.forEach((k, v) -> {
			var item = new CompletionItem();
			item.setKind(CompletionItemKind.Function);
			item.setLabel(k);
			items.put(k, item);
		});
		constants.forEach((k, v) -> {
			var item = new CompletionItem();
			item.setKind(CompletionItemKind.Constant);
			item.setLabel(k);
			items.put(k, item);
		});
		variables.forEach((k, v) -> {
			var item = new CompletionItem();
			item.setKind(CompletionItemKind.Variable);
			item.setLabel(k);
			items.put(k, item);
		});
		return items;
	}

	public boolean declare(
		Map<String, Type> environment,
		String identifier,
		Type type
	) {
		if (environment.containsKey(identifier)) {
			return false;
		}
		environment.put(identifier, type);
		return true;
	}

	public Optional<Type> lookup(String identifier) {
		return Optional.ofNullable(variables.get(identifier))
			.or(() -> Optional.ofNullable(constants.get(identifier)))
			.or(() -> Optional.ofNullable(functions.get(identifier)))
			.or(() -> Optional.ofNullable(types.get(identifier)))
			.or(() -> Optional.ofNullable(modules.get(identifier)))
			.or(() -> parent == null ? Optional.empty() : parent.lookup(identifier));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Variables

	private final Map<String, Type> variables = new HashMap<>();

	public boolean declareVariable(String identifier, Type type) {
		return declare(variables, identifier, type);
	}

	public boolean isDeclaredVariable(String identifier) {
		return variables.containsKey(identifier)
			|| (parent != null && parent.isDeclaredVariable(identifier));
	}

	public Optional<Type> getVariable(String identifier) {
		if (variables.containsKey(identifier)) {
			return Optional.of(variables.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getVariable(identifier);
	}


	//////////////////////////////////////////////////////////////////////////////////////////////// Constants

	private final Map<String, Type> constants = new HashMap<>();

	public boolean declareConstant(String identifier, Type type) {
		return declare(constants, identifier, type);
	}

	public boolean isDeclaredConstant(String identifier) {
		return constants.containsKey(identifier)
			|| (parent != null && parent.isDeclaredConstant(identifier));
	}

	public Optional<Type> getConstant(String identifier) {
		if (constants.containsKey(identifier)) {
			return Optional.of(constants.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getConstant(identifier);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Functions

	private final Map<String, Type> functions = new HashMap<>();

	public boolean declareFunction(String identifier, Type type) {
		return declare(functions, identifier, type);
	}

	public boolean isDeclaredFunction(String identifier) {
		return functions.containsKey(identifier)
			|| (parent != null && parent.isDeclaredFunction(identifier));
	}

	public Optional<Type> getFunction(String identifier) {
		if (functions.containsKey(identifier)) {
			return Optional.of(functions.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getFunction(identifier);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Types

	private final Map<String, Type> types = new HashMap<>();

	public static final Map<String, TypePrimitive> primitiveTypes = Map.of();

	public boolean declareType(String identifier, Type type) {
		return declare(types, identifier, type);
	}

	public boolean isDeclaredType(String identifier) {
		return types.containsKey(identifier)
			|| (parent != null && parent.isDeclaredType(identifier));
	}

	public Optional<Type> getType(String identifier) {
		return switch (identifier) {
			case "Natural" -> Optional.of(Type.NAT);
			case "Int" -> Optional.of(Type.INT);
			case "Real" -> Optional.of(Type.REAL);
			case "Bool" -> Optional.of(Type.BOOL);
			case "Text" -> Optional.of(Type.TEXT);
			default -> {
				if (types.containsKey(identifier)) {
					yield Optional.of(types.get(identifier));
				}
				if (parent == null) {
					yield Optional.empty();
				}
				yield parent.getType(identifier);
			}
		};
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Modules

	private final Map<String, Type> modules = new HashMap<>();

	public void declareModule(String identifier, Type type) {
		declare(modules, identifier, type);
	}

	public boolean isDeclaredModule(String identifier) {
		return modules.containsKey(identifier)
			|| (parent != null && parent.isDeclaredModule(identifier));
	}

	public Optional<Type> getModule(String identifier) {
		if (modules.containsKey(identifier)) {
			return Optional.of(modules.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getModule(identifier);
	}

}
